export function CheckInHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Check-in page records who has turned up on the day. Check-in here is what counts;
        the RaceHub arrival mark is shown beside it for reference only. The grid call in the
        cockpit lists anyone in the next race who has not checked in.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Camera scan:</span> Hold the transponder label up to the camera. QR codes, Code 128 and EAN-13 barcodes are read.</li>
        <li><span className="font-semibold">Keyboard scanner:</span> Click the scanner field and scan, or type the transponder number and press Enter.</li>
        <li><span className="font-semibold">Search:</span> Type at least two letters of the competitor&apos;s name. An unmatched scan fills the search for you.</li>
        <li><span className="font-semibold">Confirm:</span> Click Confirm check-in. A competitor racing two classes on one transponder shows one card per class.</li>
        <li><span className="font-semibold">Swap a transponder:</span> Find the entry, choose Primary or Secondary, and enter the new number. Leave the secondary blank to remove it. Laps count on the new number from the next passing.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          A number another competitor already uses in this event is refused. If two
          competitors have swapped transponders with each other, give one of them a temporary
          number first, then set both.
        </p>
      </div>
    </div>
  );
}
