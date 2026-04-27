# AMB Decoder Protocol Reference

## Overview

AMB/MyLaps RC timing decoders have used two distinct TCP protocols over their hardware lifecycle:

| Protocol | Common name | Firmware | Port | Format |
|----------|------------|----------|------|--------|
| RC-4 text | AmbRc / P9.8 / Enhanced | < 4.5 | **5100** | SOH-prefixed, tab-separated ASCII |
| P3 binary | P3 | ≥ 4.5 (or older high-end decoders) | **5403** | `0x8E`/`0x8F` framed, TLV body |

The **RC-4 text protocol** is the dominant protocol for RC club use. The majority of club decoders run firmware below 4.5 and use the text protocol. The Go prototype in `oggthemiffed/RCTimingForwarder` was written specifically for this protocol and connects to port 5100.

The **P3 binary protocol** is used by firmware 4.5+, by older high-end AMBmx3/TranX3/ChipX/ProChip decoders, and by software such as Racewave. P3 is also used on the RS232 serial interface when the decoder menu "Protocol RS232" is set to "P3" rather than "Enhanced".

---

## Protocol Selection (firmware version boundary, port numbers)

### Firmware 4.5 — the critical boundary for RC club use

MyLaps firmware 4.5 was a deliberately disruptive update:

- **Added:** simultaneous support for drone racing and RC car transponders on the same decoder.
- **Removed:** support for **all MRT transponders** (both red and blue light rechargeable MRT units), the original AMBrc DP transponder, and house handout transponders.
- **Protocol:** firmware 4.5 uses exclusively the modern RC4 protocol and is believed to use P3 binary over TCP. (⚠ Unconfirmed whether the TCP port changes in 4.5.)

**Impact:** clubs that support the MRT rechargeable transponder (the most common affordable transponder in UK/AU club racing) remain on **firmware ≤ 4.4** and therefore use the **RC-4 text protocol on port 5100**. This is the user's case.

Firmware 4.4 supports:
- MYLAPS RC4 3-wire transponders
- MYLAPS RC4 Hybrid 2-wire transponders
- Older AMBrc 2-wire transponders
- MRT rechargeable transponders (red / blue light)
- House handout transponders

### Protocol is not auto-detected

The decoder menu option "Protocol RS232" (General menu) selects the RS232 output protocol between "Enhanced" (= RC-4 text, default), "P3" (binary), and "Remote" (Orbits software mode). The TCP/IP output protocol is **not separately configurable** in the menu — the decoder serves both protocols simultaneously on different ports (5100 for text, 5403 for P3), or only the text protocol on 5100 depending on firmware version.

**For practical implementation: connect to port 5100 for firmware < 4.5 (text protocol); connect to port 5403 for firmware ≥ 4.5 or confirmed P3 decoders (binary protocol).**

---

## RC-4 Text Protocol (firmware < 4.5, port 5100)

### Overview

- **Also called:** AmbRc protocol, P9.8, Enhanced protocol, P98
- **Port:** TCP 5100. The decoder **listens**; the client connects.
- **Encoding:** 7-bit ASCII
- **Line ending:** `CR LF` (`\r\n`, i.e. `0x0D 0x0A`)
- **Field separator:** horizontal tab (`\t`, `0x09`)
- **SOH prefix:** each line begins with byte `0x01` (ASCII SOH / Start of Header)
- **No client handshake required:** the decoder begins streaming immediately after TCP connection. No greeting message is needed from the client.
- **Flow:** decoder streams records continuously. Client is read-only (no commands sent back).

### Connection & Handshake

```
Client → TCP connect to decoder on port 5100
Decoder → streams STATUS and PASSING records immediately
```

There is **no application-level handshake**. The decoder starts streaming `#` (STATUS) records every 5 seconds and `@` (PASSING) records as transponders are detected. The Go client in `RCTimingForwarder/amb_rc4_decoder_client.go` confirms this: it connects and immediately reads lines without sending anything.

**⚠ AMB20 mode note:** Very old AMBrc decoders power up in AMB20 compatibility mode (truncated transponder IDs) until AMBrc software triggers the switch to AMBrc mode. The RC-4 decoder (firmware ≤ 4.4) powers up directly in RC-4 / AmbRc mode — no mode-switch command is needed.

### Line Format

Every line has this structure:

```
SOH <type> TAB <fields...> TAB <crc> CRLF
```

Where:
- `SOH` = `0x01` (one byte, not printable)
- `<type>` = single ASCII character: `#` (STATUS) or `@` (PASSING)
- `TAB` = `0x09`
- `<crc>` = hex CRC, ASCII string prefixed with `x` (e.g. `x6B89`), no `0x` prefix
- `CRLF` = `0x0D 0x0A`

### Record Types

Two record types are observed in the captures:

| Type char | Record name | Description |
|-----------|-------------|-------------|
| `#` | STATUS | Background noise / heartbeat, sent every ~5 seconds |
| `@` | PASSING | Transponder detection event |

No other record types are observed. There is no separate WATCHDOG or RESEND in this protocol.

### STATUS Record — Field Reference

```
0x01 '#' TAB decoderID TAB seqnum TAB noiseLevel TAB unknownField TAB crc CRLF
```

| Position | Field name | Type | Units | Example | Notes |
|----------|-----------|------|-------|---------|-------|
| 0 | SOH | byte `0x01` | — | `\x01` | Fixed prefix |
| 1 | type | char `'#'` | — | `#` | Record type discriminator |
| 2 | decoder_id | uint, ASCII decimal | — | `20` | Decoder unit identifier. Value `20` observed in all captures. |
| 3 | seq_num | uint, ASCII decimal | — | `0`, `1`, `2563` | Monotonically increasing per record (shared with PASSING). Wraps at max uint. |
| 4 | noise_level | uint, ASCII decimal | raw 0–255+ | `72`, `73`, `64` | Background loop noise. Target < 40 for good install. |
| 5 | unknown | uint, ASCII decimal | — | `0` | Always `0` in all observed records. Possibly unused or reserved field. |
| 6 | crc | hex string | — | `x6B89` | CRC of record. Prefixed with literal `x`, no `0x`. 4 hex digits. |

**Example (raw, with `^A`=SOH, `^I`=TAB, `^M`=CR):**
```
^A#^I20^I0^I72^I0^Ix6B89^M
```

**Example (decoded):**
```
# | decoder_id=20 | seq=0 | noise=72 | unknown=0 | crc=x6B89
```

STATUS records arrive approximately every **5 seconds**. They serve as a heartbeat — if no STATUS record arrives for > 30 seconds, assume the decoder connection is lost.

### PASSING Record — Field Reference

```
0x01 '@' TAB decoderID TAB seqnum TAB transponderCode TAB timeSinceStart TAB hitCounts TAB signalStrength TAB passingStatus TAB crc CRLF
```

| Position | Field name | Type | Units | Example | Notes |
|----------|-----------|------|-------|---------|-------|
| 0 | SOH | byte `0x01` | — | `\x01` | Fixed prefix |
| 1 | type | char `'@'` | — | `@` | Record type discriminator |
| 2 | decoder_id | uint, ASCII decimal | — | `20` | Same decoder ID as STATUS |
| 3 | seq_num | uint, ASCII decimal | — | `2`, `7`, `2575` | Shared sequence counter with STATUS records |
| 4 | transponder_code | uint, ASCII decimal | — | `8533156`, `3885036` | Transponder hardware ID (chip number). Two distinct transponders visible in captures. |
| 5 | time_since_start | float, ASCII decimal | **seconds** | `596.126`, `1164.389` | **Seconds since decoder power-on.** Not a Unix timestamp. See §Timestamp Handling. |
| 6 | hit_counts | uint, ASCII decimal | count | `400`, `266` | Number of loop crossings during passing window. Higher = slower / better signal. |
| 7 | signal_strength | uint, ASCII decimal | raw 0–255 | `163`, `107` | Signal strength of transponder. Target > 60 above noise. |
| 8 | passing_status | uint, ASCII decimal | — | `2` | Always `2` in all observed records. Meaning unclear; likely "valid passing". |
| 9 | crc | hex string | — | `x14F5` | CRC. Same format as STATUS. |

**Example (raw):**
```
^A@^I20^I2^I8533156^I596.126^I400^I163^I2^Ix14F5^M
```

**Example (decoded):**
```
@ | decoder_id=20 | seq=2 | transponder=8533156 | time=596.126s | hits=400 | strength=163 | status=2 | crc=x14F5
```

### Full Annotated Sample (from `dump_amb_ip.dump`)

```
# 20  0    72  0   x6B89     ← STATUS:  seq=0,   noise=72
# 20  1    73  0   xF2E9     ← STATUS:  seq=1,   noise=73
@ 20  2    8533156  596.126  400  163  2  x14F5  ← PASSING: transponder 8533156 at 596.126s
# 20  3    73  0   xB66A     ← STATUS:  seq=3,   noise=73
# 20  4    72  0   xE28F     ← STATUS:  seq=4,   noise=72
@ 20  7    8533156  614.635  389  157  2  xF896  ← PASSING: same transponder at 614.635s
@ 20  10   8533156  625.565  252  102  2  x31C9  ← PASSING: lap time = 625.565 - 614.635 = 10.930s
@ 20  22   8533156  671.684  392  158  2  xA648  ← PASSING: lap time = 671.684 - ...
@ 20  27   3885036  688.41   70   103  2  x4C27  ← PASSING: second transponder appears
```

Two transponder IDs appear in the captures: `8533156` and `3885036`. Lap times calculated from successive `time_since_start` values for the same transponder.

### Timestamp Handling — Converting `time_since_start` to Wall Clock

This is the **most critical implementation concern** for the text protocol.

#### What `time_since_start` is

`time_since_start` is a **floating-point number of seconds elapsed since the decoder was powered on** (or possibly since last decoder reset/session start). It has **millisecond precision** (3 decimal places observed, e.g. `596.126`, `1175.823`).

**It is NOT a Unix timestamp.** It contains no date or absolute time reference.

#### How the decoder reports its power-on reference

The RC-4 text protocol does **not** transmit an explicit power-on reference timestamp in a separate "sync" message. The decoder has no facility to tell the client "I was started at wall-clock time X".

There are three approaches to wall-clock anchoring:

**Option A — Server receipt time (simplest, recommended for club use)**

Record the server's wall-clock time (`Instant.now()`) at the moment the first (or any) line is received. All subsequent timestamps are offsets from that reference. This introduces ≤ network RTT error (< 1ms on LAN) and is perfectly adequate for RC lap timing where lap times are > 8 seconds.

```
Instant decoderEpoch = null;

void onFirstRecord(double timeSinceStart) {
    // Anchor: server clock - reported offset = decoder power-on instant
    decoderEpoch = Instant.now().minusMillis((long)(timeSinceStart * 1000));
}

Instant toWallClock(double timeSinceStart) {
    return decoderEpoch.plusMillis((long)(timeSinceStart * 1000));
}
```

**Option B — Use the first PASSING record in a race session as the lap zero reference**

For race timing purposes the absolute wall-clock time of a lap is less important than the elapsed race time and lap durations. Record the `time_since_start` of the race start signal as `raceStartTime` and compute `elapsedRaceTime = timeSinceStart - raceStartTime` for each passing. This is entirely relative and avoids the wall-clock problem.

**Option C — GPS-sync (not available in RC-4 text protocol)**

The P3 binary protocol's `RTC_TIME` field is a GPS/NTP-synchronised UTC microsecond timestamp. The RC-4 text protocol has no equivalent. If GPS-precision timestamps are required, upgrade to P3 (firmware ≥ 4.5 or P3-capable decoder).

#### Precision

The `time_since_start` field has 3 decimal places = **1ms resolution** in the text protocol. This is consistent with the decoder's 3ms timebase listed in official specifications. Do not assume sub-millisecond accuracy.

#### Decoder clock drift

The decoder's internal oscillator can drift a few milliseconds per minute. For a 5-minute RC heat (the typical race duration), drift is negligible for lap timing purposes.

### Heartbeat / Keepalive

STATUS (`#`) records are sent by the decoder every **~5 seconds** regardless of transponder activity. They serve as a heartbeat. If no STATUS record arrives within 30 seconds, declare the decoder connection dead and attempt reconnection.

There is no client-to-decoder keepalive in the RC-4 text protocol. The decoder simply pushes records; the client reads them.

### CRC Format

The CRC field is the last tab-delimited field on each line. Format: literal ASCII character `x` followed by 4 uppercase hexadecimal digits. Example: `x6B89`, `xF2E9`, `x14F5`.

The CRC algorithm is **not documented in any available open-source reference**. The Go implementation in `RCTimingForwarder/amb_rc4_processor.go` and the Android `P98Parser.kt` both declare CRC checking as unimplemented (returning `true` / not validating). For initial implementation, log CRC values but do not reject records on mismatch.

---

## AMB P3 Binary Protocol (firmware ≥ 4.5 or P3-capable decoders, port 5403)

### Overview

- **Port:** TCP 5403. The decoder **listens**; the client connects. Maximum 4 simultaneous connections.
- **Encoding:** Binary, little-endian integers
- **Frame delimiters:** `0x8E` (start) / `0x8F` (end)
- **Byte stuffing:** `0x8D` escape byte used inside frame bodies
- **No client handshake required:** decoder begins streaming immediately on connect

### Frame Structure

#### Delimiters

| Role | Byte value |
|------|-----------|
| Frame start | `0x8E` |
| Frame end | `0x8F` |

#### Byte Stuffing (Escaping)

Any occurrence of `0x8D`, `0x8E`, or `0x8F` inside the frame body is replaced:

| Raw byte | Transmitted as |
|----------|---------------|
| `0x8D` | `0x8D 0xAD` |
| `0x8E` | `0x8D 0xAE` |
| `0x8F` | `0x8D 0xAF` |

Rule: prefix with `0x8D`, add `0x20` to original byte value. On receipt: remove `0x8D`, subtract `0x20` from following byte. Unescape the entire body **after** extracting the complete frame, before parsing.

#### Header Layout (10 bytes)

```
Offset  Size  Field
------  ----  -----
0x00    1     Frame start: 0x8E
0x01    1     Version (observed: 0x02)
0x02    2     Total frame length, little-endian (includes 0x8E and 0x8F bytes)
0x04    2     CRC-16, little-endian (algorithm unconfirmed)
0x06    2     Flags header, little-endian (always 0x0000 in observed records)
0x08    2     Record type, little-endian
0x0A    var   TLV body fields
last    1     Frame end: 0x8F
```

#### TLV Body Fields

```
+--------+--------+--------...--------+
|field_id| length |     data bytes    |
| 1 byte | 1 byte |   length bytes    |
+--------+--------+--------...--------+
```

Iterate from offset `0x0A`. Stop when `pos >= frameLength - 1`. Advance by `2 + length` per field.

#### Annotated Frame Example (PASSING record)

```
8E 02 33 00 9E C7 00 00 01 00        ← 10-byte header (type=0x0001 = PASSING)
01 04 48 BC 01 00                    field 0x01 (PASSING_NUMBER), len=4, value=113736
03 04 1D 58 2C 00                    field 0x03 (TRANSPONDER),    len=4, value=2906141
04 08 E8 05 BF C4 A4 1B 05 00        field 0x04 (RTC_TIME),       len=8, value=1437769372993000 µs
05 02 6A 00                          field 0x05 (STRENGTH),       len=2, value=106
06 02 32 00                          field 0x06 (HITS),           len=2, value=50
08 02 00 00                          field 0x08 (FLAGS),          len=2, value=0
81 04 93 0F 02 00                    field 0x81 (DECODER_ID),     len=4, value=135059
8F                                   ← frame end
```

### Record Types

| Record name | Type (LE uint16) | Direction | Description |
|-------------|-----------------|-----------|-------------|
| `RESET` | `0x0000` | Decoder → Client | Decoder reset notification |
| `PASSING` | `0x0001` | Decoder → Client | Transponder detection (primary timing record) |
| `STATUS` | `0x0002` | Decoder → Client | Periodic hardware status (noise, temperature, voltage) |
| `VERSION` | `0x0003` | Client → Decoder | Request firmware/type information |
| `RESEND` | `0x0004` | Client → Decoder | Request retransmission of missing passings |
| `CLEAR_PASSING` | `0x0005` | Client → Decoder | Clear passing history |
| `SERVER_SETTINGS` | `0x0013` | Bidirectional | Server/decoder settings exchange |
| `SESSION` | `0x0015` | Bidirectional | Session management |
| `WATCHDOG` | `0x0018` | Decoder → Client | Periodic keepalive |
| `PING` | `0x0020` | Bidirectional | Connection health check |
| `FIRST_CONTACT` | `0x0045` | Decoder → Client | Initial connection identification |
| `ERROR` | `0xFFFF` | Decoder → Client | Protocol error |

### PASSING Record Fields

| Field name | Field ID | Type | Units | Notes |
|------------|----------|------|-------|-------|
| `PASSING_NUMBER` | `0x01` | uint32 LE | — | Monotonic counter per decoder. Use to detect gaps. |
| `TRANSPONDER` | `0x03` | uint32 LE | — | Transponder hardware ID |
| `RTC_TIME` | `0x04` | uint64 LE | **microseconds since Unix epoch (UTC)** | Hardware clock. GPS/NTP synchronised. |
| `STRENGTH` | `0x05` | uint16 LE | raw | Signal strength. May be absent. |
| `HITS` | `0x06` | uint16 LE | count | Loop crossings during passing window |
| `FLAGS` | `0x08` | uint16 LE | bitmask | Always 0 in observed records |

### STATUS Record Fields

| Field name | Field ID | Type | Units | Notes |
|------------|----------|------|-------|-------|
| `NOISE` | `0x01` | uint16 LE | raw | Background loop noise |
| `GPS` | `0x06` | uint8 | boolean | 0=no fix, 1=fix |
| `TEMPERATURE` | `0x07` | uint16 LE | °C | Ambient temperature |
| `INPUT_VOLTAGE` | `0x0C` | uint8 | 0.1 V | Divide by 10 for volts (119 → 11.9V) |

### General Fields (appear in multiple record types)

| Field name | Field ID | Type | Notes |
|------------|----------|------|-------|
| `DECODER_ID` | `0x81` | uint32 LE | Hardware serial. Consistent across all records from a decoder. |

### Timestamp Format

`RTC_TIME` is a **uint64 little-endian** count of **microseconds since the Unix epoch (UTC)**.

```java
long rtcTimeMicros = /* uint64 little-endian from wire */;
Instant timestamp = Instant.ofEpochSecond(
    rtcTimeMicros / 1_000_000L,
    (rtcTimeMicros % 1_000_000L) * 1_000L  // convert remainder to nanoseconds
);
```

Confirmed by PHP parser test suite: `RTC_TIME = 1437769372993000` → `2015-07-24T20:22:52.993000Z`.

Do **not** use server receipt time. Always record `RTC_TIME` from the passing frame.

### FIRST_CONTACT Handshake

`FIRST_CONTACT` is record type `0x0045`. The decoder does **not** require a client-initiated FIRST_CONTACT. The decoder begins pushing STATUS and PASSING records immediately on TCP connect.

**Recommended approach:**
```
Client → TCP connect to port 5403
Decoder → streams STATUS and PASSING records immediately
Client → (optional) send VERSION request (type 0x0003) to identify decoder firmware
```

If a `FIRST_CONTACT` record is received from the decoder, parse the header for type/decoder ID, discard the body.

### Gap Detection & RESEND

Monitor `PASSING_NUMBER` (field `0x01` in PASSING records) for sequence gaps:

```java
int lastPassingNumber = -1;
void onPassingReceived(int passingNumber) {
    if (lastPassingNumber >= 0 && passingNumber != lastPassingNumber + 1) {
        int missing = passingNumber - lastPassingNumber - 1;
        log.warn("Gap: missing {} passings from {} to {}",
                 missing, lastPassingNumber + 1, passingNumber - 1);
        // sendResendRequest(...) — structure unconfirmed, obtain from MyLaps SDK
    }
    lastPassingNumber = passingNumber;
}
```

⚠ The exact TLV structure of a RESEND frame is unconfirmed in open sources. Register at `mylaps.com/developers` for the official SDK before implementing RESEND.

### WATCHDOG

`WATCHDOG` is record type `0x0018`. Sent by the decoder at regular intervals as a keepalive.

**Absence threshold:** if no frame of any type is received within **30 seconds**, declare connection lost.

```java
// Netty IdleStateHandler
pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
```

---

## Implementation Architecture

### `TimingSource` Interface

Both protocols expose the same event to the application: a transponder was detected at a specific point in time. A single interface covers both:

```java
public interface TimingSource {
    /** Start listening for events. Called once on startup. */
    void start();
    /** Stop gracefully. */
    void stop();
}

public record LapPassingEvent(
    long    transponderId,   // chip ID from hardware
    Instant timestamp,       // wall-clock UTC (from RTC_TIME or server-anchored offset)
    int     signalStrength,  // raw units
    int     hitCount,        // number of loop crossings
    int     decoderId,       // hardware decoder ID
    long    sequenceNumber   // per-decoder monotonic counter
) {}
```

### Dual-Protocol Selector

```
TimingSourceFactory.create(config)
  ├── config.port == 5100 → Rc4TextTimingSource  (Netty, reads lines until \n)
  └── config.port == 5403 → P3BinaryTimingSource (Netty, ByteToMessageDecoder)
```

Or auto-detect by attempting port 5100 first (RC-4 text); fall back to 5403 (P3 binary) on failure. The recommended approach for club use is to make the port configurable.

### RC-4 Text Protocol — Java Implementation Notes

1. **Parser must be a pure function** — `String line → Optional<LapPassingEvent>`. No Spring dependencies. No I/O.

2. **Netty LineBasedFrameDecoder** is appropriate: the protocol is line-oriented (`\n` or `\r\n` terminated). Use `LineBasedFrameDecoder` followed by a `StringDecoder` (US_ASCII), then the application handler.

3. **SOH prefix handling:** the `0x01` byte is at position 0 of each line. Strip it before splitting on TAB.

4. **Time anchoring:** record `Instant.now()` when the first record arrives and subtract `timeSinceStart` seconds to derive `decoderEpoch`. Apply to all subsequent records.

5. **Sequence monitoring:** track the last seen `seq_num` across both STATUS and PASSING records (they share the same counter). A gap in sequence numbers does not indicate a retransmit opportunity — there is no RESEND in the text protocol. Log the gap.

6. **CRC:** log but do not reject. CRC algorithm is unconfirmed.

```java
// Skeleton RC-4 text parser (pure function — no Spring)
public class Rc4TextParser {
    private static final int SOH = 0x01;

    public Optional<LapPassingEvent> parse(String rawLine) {
        if (rawLine.isEmpty() || rawLine.charAt(0) != SOH) return Optional.empty();
        String line = rawLine.substring(1);  // strip SOH
        String[] fields = line.split("\t");
        return switch (fields[0]) {
            case "@" -> parsePassing(fields);
            case "#" -> Optional.empty();  // STATUS — used for heartbeat only
            default  -> Optional.empty();
        };
    }

    private Optional<LapPassingEvent> parsePassing(String[] f) {
        // f[0]='@' f[1]=decoderId f[2]=seqNum f[3]=transponderId f[4]=timeSinceStart
        // f[5]=hits f[6]=strength f[7]=passingStatus f[8]=crc
        if (f.length < 9) return Optional.empty();
        return Optional.of(new LapPassingEvent(
            Long.parseLong(f[3]),          // transponderId
            null,                          // timestamp — anchored externally by TimingSource
            Integer.parseInt(f[6]),        // signalStrength
            Integer.parseInt(f[5]),        // hitCount
            Integer.parseInt(f[1]),        // decoderId
            Long.parseLong(f[2])           // sequenceNumber
        ));
    }
}
```

### P3 Binary Protocol — Java Implementation Notes

See detailed framing notes in previous §§. Use `ByteToMessageDecoder` in Netty:

1. Buffer bytes until `0x8F` is seen.
2. Extract the complete frame (`0x8E` ... `0x8F`).
3. Unescape only the body (bytes [1 .. len-2]).
4. Parse the 10-byte header: version, length, CRC, flags, record type.
5. Iterate TLV fields from offset `0x0A`.

### TCP Simulator for Development

Implement a `FakeDecoder` that listens on a configurable port and emits:
- STATUS records every 5 seconds (text) or WATCHDOG every 10 seconds (P3)
- Scripted PASSING records from a CSV file of `(transponder, timeSecs)` tuples

The simulator enables full end-to-end testing of the forwarder without physical hardware.

---

## Field Comparison: RC-4 Text vs P3 Binary

| Concept | RC-4 text field | P3 binary field |
|---------|----------------|----------------|
| Transponder ID | `transponder_code` (decimal ASCII) | `TRANSPONDER` (uint32 LE) |
| Timestamp | `time_since_start` (float seconds since power-on) | `RTC_TIME` (uint64 µs since Unix epoch) |
| Signal strength | `signal_strength` (uint, raw) | `STRENGTH` (uint16 LE) |
| Hit count | `hit_counts` (uint) | `HITS` (uint16 LE) |
| Sequence number | `seq_num` (shared STATUS+PASSING counter) | `PASSING_NUMBER` (PASSING-only counter) |
| Decoder ID | `decoder_id` (uint decimal, field 2) | `DECODER_ID` (TLV field 0x81, uint32 LE) |
| Heartbeat | STATUS (`#`) every 5s | WATCHDOG (type 0x0018) |
| Gap recovery | None — no RESEND | RESEND (type 0x0004) |
| Absolute time | No — offset only | Yes — UTC microseconds |

---

## Sources

| Source | Used for |
|--------|----------|
| `oggthemiffed/RCTimingForwarder` `samples/dump_amb_ip.dump` | RC-4 text protocol IP capture — 44 lines, two transponders, field values confirmed |
| `oggthemiffed/RCTimingForwarder` `samples/dump_amb_serial.dump` | RC-4 text protocol serial capture — identical format, higher sequence numbers |
| `oggthemiffed/RCTimingForwarder` `amb_rc4_decoder_client.go` | TCP port 5100 confirmed, line-reading loop |
| `oggthemiffed/RCTimingForwarder` `amb_rc4_processor.go` | Field annotations (decoder_id, seq_num, transponder_code, time_since_start, hit_counts, signal_strength, passing_status, CRC) |
| `oggthemiffed/RCTimingForwarder` `main.go` | Port 5100, decoder IP 172.20.0.217 |
| `skoky/ammc-android` `P98Parser.kt` | P98/RC-4 text parser — field count per record type (STATUS 5 fields, PASSING 9 fields), tab separator, CRC format, time_since_start × 1000 = milliseconds |
| `skoky/ammc-android` `Tools.kt` | `P3_DEF_PORT = 5403` confirmed |
| `datagutten/amb-p3-parser` `src/parser.php` | Authoritative P3 frame structure, record types, TLV field IDs, byte stuffing |
| `datagutten/amb-p3-parser` test suite | RTC_TIME Unix µs epoch confirmed; PASSING_NUMBER values; DECODER_ID |
| AMB decoder manual (2006, `transponderservices.com`) | Protocol RS232 menu: Enhanced / P3 / Remote options; P3 = preferred for developers; Enhanced = backward compat; "First contact" option only in P3 protocol |
| MYLAPS decoder manual (`mxtransponder.com`, Jan 2010) | Same Protocol RS232 menu documented; dataserver menu (host+port for cloud upload) |
| AMBrc manual Rev.1.1b (FCC filing) | Power-up in AMB20 mode; AMBrc software triggers AMBrc mode switch; STATUS every 5 seconds; RS232 connection |
| MYLAPS RC manual (Jan 2010, `transponderservices.com`) | RC-4 system overview; RS232/TCP connection cable listed |
| LiveRC news article | Firmware 4.5: disables MRT transponders, adds drone support, uses RC4 protocol exclusively |
| AMBrc manual Rev.3.3 (`transponderservices.com`) | Decoder startup behaviour; noise/signal/hit descriptions |
| `datagutten/amb-p3-parser` Packagist page | Port 5403; max 4 connections; P3 only |
