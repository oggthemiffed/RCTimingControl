import { useEffect, useState } from 'react';
import { useHelp } from '@/context/HelpContext';
import { RacerProfileHelp } from '@/help/RacerProfileHelp';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { Loader2, Trash2, Play } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import {
  Form, FormField, FormItem, FormLabel, FormControl, FormMessage,
} from '@/components/ui/form';
import { Separator } from '@/components/ui/separator';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  useProfile, useUpdateProfile, useAddMembership, useRemoveMembership, useAffiliations,
} from '@/hooks/racer/useProfile';
import { useQuery } from '@tanstack/react-query';
import { listVoices, previewNameClip, saveVoicePreference } from '@/lib/audioApi';

const profileSchema = z.object({
  firstName: z.string().min(1, 'Required').max(100),
  lastName:  z.string().min(1, 'Required').max(100),
  phoneNumber:           z.string().max(30).optional().or(z.literal('')),
  emergencyContactName:  z.string().max(100).optional().or(z.literal('')),
  emergencyContactPhone: z.string().max(30).optional().or(z.literal('')),
  phoneticName:          z.string().max(255).optional().or(z.literal('')),
});
type ProfileForm = z.infer<typeof profileSchema>;

const membershipSchema = z.object({
  governingBodyCode: z.string().min(1, 'Required').max(20),
  membershipNumber:  z.string().min(1, 'Required').max(50),
});
type MembershipForm = z.infer<typeof membershipSchema>;

export default function ProfilePage() {
  const { data: profile, isPending, error } = useProfile();
  const { data: affiliations = [] } = useAffiliations();
  const updateProfile = useUpdateProfile();
  const addMembership = useAddMembership();
  const removeMembership = useRemoveMembership();
  const { setHelpContent } = useHelp();

  useEffect(() => {
    setHelpContent(<RacerProfileHelp />);
    return () => setHelpContent(null);
  }, [setHelpContent]);

  // ── Voice preference state ─────────────────────────────────────────────────
  const { data: voices, isLoading: voicesLoading } = useQuery({
    queryKey: ['voices'],
    queryFn: () => listVoices().then((r) => r.data),
  });
  const [selectedVoice, setSelectedVoice] = useState<string>('');
  const [isPreviewPlaying, setIsPreviewPlaying] = useState(false);

  useEffect(() => {
    if (profile?.preferredVoiceId && !selectedVoice) {
      setSelectedVoice(profile.preferredVoiceId);
    }
  }, [profile?.preferredVoiceId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handlePreview = async () => {
    setIsPreviewPlaying(true);
    try {
      const response = await previewNameClip(selectedVoice || undefined);
      const url = URL.createObjectURL(response.data);
      const audio = new Audio(url);
      audio.onended = () => {
        setIsPreviewPlaying(false);
        URL.revokeObjectURL(url);
      };
      await audio.play();
    } catch {
      toast.error('Could not generate preview. Please try again.');
      setIsPreviewPlaying(false);
    }
  };

  const handleSaveVoice = async () => {
    try {
      await saveVoicePreference(selectedVoice || null);
      toast.success('Voice preference saved.');
    } catch {
      toast.error('Failed to save voice preference.');
    }
  };

  const form = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    mode: 'onBlur',
    defaultValues: {
      firstName: '', lastName: '', phoneNumber: '',
      emergencyContactName: '', emergencyContactPhone: '', phoneticName: '',
    },
  });

  useEffect(() => {
    if (profile) {
      form.reset({
        firstName: profile.firstName ?? '',
        lastName:  profile.lastName ?? '',
        phoneNumber:           profile.phoneNumber ?? '',
        emergencyContactName:  profile.emergencyContactName ?? '',
        emergencyContactPhone: profile.emergencyContactPhone ?? '',
        phoneticName:          profile.phoneticName ?? '',
      });
    }
  }, [profile, form]);

  async function onSubmit(values: ProfileForm) {
    try {
      await updateProfile.mutateAsync({
        firstName: values.firstName,
        lastName:  values.lastName,
        phoneNumber:           values.phoneNumber || undefined,
        emergencyContactName:  values.emergencyContactName || undefined,
        emergencyContactPhone: values.emergencyContactPhone || undefined,
        phoneticName:          values.phoneticName,
      });
      toast.success('Profile updated');
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 400) {
        const errors = err.response.data?.errors as Record<string, string> | undefined;
        if (errors) {
          Object.entries(errors).forEach(([field, msg]) =>
            form.setError(field as keyof ProfileForm, { message: msg }));
          return;
        }
      }
      toast.error('Failed to save profile. Please try again.', { duration: 8000 });
    }
  }

  const membershipForm = useForm<MembershipForm>({
    resolver: zodResolver(membershipSchema),
    defaultValues: {
      governingBodyCode: affiliations[0]?.code ?? '',
      membershipNumber: '',
    },
  });

  useEffect(() => {
    if (affiliations.length > 0 && !membershipForm.getValues('governingBodyCode')) {
      membershipForm.setValue('governingBodyCode', affiliations[0].code);
    }
  }, [affiliations, membershipForm]);

  async function onAddMembership(values: MembershipForm) {
    try {
      await addMembership.mutateAsync(values);
      membershipForm.reset();
      toast.success('Membership added.');
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 409) {
        toast.error('Already registered with this body.');
        return;
      }
      toast.error('Unable to add membership. Please try again.', { duration: 8000 });
    }
  }

  if (isPending) {
    return (
      <div aria-live="polite" className="max-w-2xl mx-auto space-y-8">
        <div className="animate-pulse bg-muted rounded h-48" />
        <div className="animate-pulse bg-muted rounded h-32" />
        <div className="animate-pulse bg-muted rounded h-24" />
      </div>
    );
  }
  if (error) {
    return <div role="alert" className="text-destructive">Unable to load profile. Please refresh.</div>;
  }

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      {/* Personal Details + Emergency Contact */}
      <Card>
        <CardHeader><CardTitle>Profile</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              {/* Email — read-only, identity is fixed */}
              <div>
                <label className="text-sm font-medium">Email</label>
                <Input value={profile!.email} disabled readOnly className="mt-1" />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <FormField control={form.control} name="firstName" render={({ field }) => (
                  <FormItem>
                    <FormLabel>First name</FormLabel>
                    <FormControl><Input {...field} /></FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="lastName" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Last name</FormLabel>
                    <FormControl><Input {...field} /></FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <FormField control={form.control} name="phoneticName" render={({ field }) => (
                <FormItem>
                  <FormLabel>Phonetic name</FormLabel>
                  <FormControl><Input {...field} value={field.value ?? ''} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />

              <FormField control={form.control} name="phoneNumber" render={({ field }) => (
                <FormItem>
                  <FormLabel>Phone number</FormLabel>
                  <FormControl><Input type="tel" {...field} value={field.value ?? ''} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />

              <Separator />
              <p className="text-sm font-medium text-muted-foreground">Emergency contact</p>

              <FormField control={form.control} name="emergencyContactName" render={({ field }) => (
                <FormItem>
                  <FormLabel>Contact name</FormLabel>
                  <FormControl><Input {...field} value={field.value ?? ''} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />

              <FormField control={form.control} name="emergencyContactPhone" render={({ field }) => (
                <FormItem>
                  <FormLabel>Contact phone</FormLabel>
                  <FormControl><Input type="tel" {...field} value={field.value ?? ''} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />

              <Button
                type="submit"
                disabled={!form.formState.isDirty || updateProfile.isPending}
              >
                {updateProfile.isPending
                  ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Saving…</>
                  : 'Save changes'}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>

      {/* Governing Body Memberships */}
      <Card>
        <CardHeader><CardTitle>Governing body memberships</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {profile!.memberships.length === 0 && (
            <p className="text-muted-foreground text-sm">
              No memberships on file. Add your governing body membership number below.
            </p>
          )}
          {profile!.memberships.length > 0 && (
            <ul className="space-y-2">
              {profile!.memberships.map((m) => (
                <li key={m.governingBodyCode} className="flex items-center justify-between">
                  <span>
                    <Badge variant="secondary" className="mr-2">{m.governingBodyCode}</Badge>
                    {m.membershipNumber}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Remove ${m.governingBodyCode} membership`}
                    onClick={() => removeMembership.mutate(m.governingBodyCode)}
                    disabled={removeMembership.isPending}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </li>
              ))}
            </ul>
          )}
          <Separator />
          <Form {...membershipForm}>
            <form onSubmit={membershipForm.handleSubmit(onAddMembership)} className="flex gap-2 items-end">
              <FormField control={membershipForm.control} name="governingBodyCode" render={({ field }) => (
                <FormItem className="flex-1">
                  <FormLabel>Body</FormLabel>
                  <FormControl>
                    {affiliations.length > 0 ? (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select body" />
                        </SelectTrigger>
                        <SelectContent>
                          {affiliations.map((a) => (
                            <SelectItem key={a.code} value={a.code}>
                              {a.displayName} ({a.code})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    ) : (
                      <Input placeholder="e.g. BRCA" {...field} />
                    )}
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={membershipForm.control} name="membershipNumber" render={({ field }) => (
                <FormItem className="flex-1">
                  <FormLabel>Number</FormLabel>
                  <FormControl><Input {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <Button type="submit" disabled={addMembership.isPending}>Add</Button>
            </form>
          </Form>
        </CardContent>
      </Card>

      {/* Ability Ratings — read-only */}
      <Card>
        <CardHeader><CardTitle>Ability ratings</CardTitle></CardHeader>
        <CardContent>
          {profile!.classRatings.length === 0
            ? <p className="text-muted-foreground text-sm">No ability ratings assigned yet.</p>
            : <ul className="space-y-2">
                {profile!.classRatings.map((r) => (
                  <li key={r.racingClassId} className="flex items-center justify-between gap-4">
                    <span className="text-sm">Class #{r.racingClassId}</span>
                    <div className="flex items-center gap-2 flex-1">
                      <div className="flex-1 bg-muted rounded h-2 overflow-hidden">
                        <div
                          className="h-2 bg-primary/30 rounded"
                          style={{ width: `${r.rating}%` }}
                        />
                      </div>
                      <Badge variant="secondary" className="shrink-0">{r.rating}</Badge>
                    </div>
                  </li>
                ))}
              </ul>}
        </CardContent>
      </Card>

      {/* Announcement Voice */}
      <Card>
        <CardHeader><CardTitle>Announcement Voice</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="voice-select">Voice</Label>
            <Select value={selectedVoice} onValueChange={setSelectedVoice}>
              <SelectTrigger id="voice-select">
                <SelectValue
                  placeholder={voicesLoading ? 'Loading voices…' : 'Select voice'}
                />
              </SelectTrigger>
              <SelectContent>
                {voices?.map((v) => (
                  <SelectItem key={v.voiceId} value={v.voiceId}>
                    {v.label}{v.isDefault ? ' — club default' : ''}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              Your chosen voice will be used when announcing your name during races.
            </p>
          </div>

          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handlePreview}
              disabled={isPreviewPlaying}
              aria-label="Preview voice"
            >
              {isPreviewPlaying ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
              ) : (
                <Play className="mr-2 h-4 w-4" aria-hidden="true" />
              )}
              Preview
            </Button>
            <Button size="sm" onClick={handleSaveVoice}>
              Save Voice Preferences
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
