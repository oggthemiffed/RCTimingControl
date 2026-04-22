import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import type { ChampionshipDto, ScoringSource } from '@/lib/adminApi';

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  scoringSource: z.enum(['QUALIFYING', 'FINALS', 'BOTH']),
  bestXFromYX: z.coerce.number().int().positive().nullable(),
  bestXFromYY: z.coerce.number().int().positive().nullable(),
  tqBonusPoints: z.coerce.number().int().min(0),
  afinalWinnerBonusPoints: z.coerce.number().int().min(0),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  initialValue?: Partial<ChampionshipDto>;
  onSubmit: (body: Omit<ChampionshipDto, 'id'>) => Promise<unknown>;
  submitLabel: string;
}

export function ChampionshipConfigForm({ initialValue, onSubmit, submitLabel }: Props) {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: initialValue?.name ?? '',
      scoringSource: (initialValue?.scoringSource as ScoringSource) ?? 'QUALIFYING',
      bestXFromYX: initialValue?.bestXFromYX ?? null,
      bestXFromYY: initialValue?.bestXFromYY ?? null,
      tqBonusPoints: initialValue?.tqBonusPoints ?? 0,
      afinalWinnerBonusPoints: initialValue?.afinalWinnerBonusPoints ?? 0,
    },
  });

  async function handleFormSubmit(values: FormValues) {
    await onSubmit({
      name: values.name,
      scoringSource: values.scoringSource,
      bestXFromYX: values.bestXFromYX,
      bestXFromYY: values.bestXFromYY,
      tqBonusPoints: values.tqBonusPoints,
      afinalWinnerBonusPoints: values.afinalWinnerBonusPoints,
    });
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-5 max-w-md">
      <div className="space-y-1.5">
        <Label htmlFor="champ-name">Name</Label>
        <Input id="champ-name" {...register('name')} placeholder="e.g. 2026 Club Championship" />
        {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
      </div>

      <div className="space-y-1.5">
        <Label>Scoring Source</Label>
        <Controller
          name="scoringSource"
          control={control}
          render={({ field }) => (
            <RadioGroup
              value={field.value}
              onValueChange={field.onChange}
              className="flex gap-4"
            >
              {(['QUALIFYING', 'FINALS', 'BOTH'] as ScoringSource[]).map(val => (
                <div key={val} className="flex items-center gap-2">
                  <RadioGroupItem value={val} id={`scoring-${val}`} />
                  <Label htmlFor={`scoring-${val}`} className="font-normal capitalize">
                    {val.charAt(0) + val.slice(1).toLowerCase().replace('_', ' ')}
                  </Label>
                </div>
              ))}
            </RadioGroup>
          )}
        />
      </div>

      <div className="space-y-1.5">
        <Label>Best X from Y (optional)</Label>
        <div className="flex items-center gap-2">
          <Input
            type="number"
            className="w-24"
            placeholder="X"
            {...register('bestXFromYX')}
          />
          <span className="text-muted-foreground text-sm">from</span>
          <Input
            type="number"
            className="w-24"
            placeholder="Y"
            {...register('bestXFromYY')}
          />
          <span className="text-sm text-muted-foreground">rounds</span>
        </div>
        {(errors.bestXFromYX || errors.bestXFromYY) && (
          <p className="text-xs text-destructive">Must be positive integers if set</p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="tq-bonus">TQ Bonus Points</Label>
          <Input id="tq-bonus" type="number" min={0} {...register('tqBonusPoints')} />
          {errors.tqBonusPoints && (
            <p className="text-xs text-destructive">{errors.tqBonusPoints.message}</p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="afinal-bonus">A-Final Winner Bonus</Label>
          <Input id="afinal-bonus" type="number" min={0} {...register('afinalWinnerBonusPoints')} />
          {errors.afinalWinnerBonusPoints && (
            <p className="text-xs text-destructive">{errors.afinalWinnerBonusPoints.message}</p>
          )}
        </div>
      </div>

      <Button type="submit" disabled={isSubmitting || (!isDirty && !!initialValue)}>
        {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : null}
        {submitLabel}
      </Button>
    </form>
  );
}
