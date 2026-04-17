import { useState } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useCars } from '@/hooks/racer/useCars';
import CarCard from '@/components/racer/CarCard';
import CarEditSheet from '@/components/racer/CarEditSheet';
import type { CarDto } from '@/lib/racerApi';

export default function CarsPage() {
  const { data: cars, isPending, error } = useCars();
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<CarDto | null>(null);

  function openCreate() { setSelected(null); setOpen(true); }
  function openEdit(car: CarDto) { setSelected(car); setOpen(true); }

  if (isPending) {
    return (
      <div aria-live="polite" className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-4">
          <div className="animate-pulse bg-muted rounded h-8 w-24" />
          <div className="animate-pulse bg-muted rounded h-9 w-24" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="animate-pulse bg-muted rounded h-32" />
          ))}
        </div>
      </div>
    );
  }
  if (error) {
    return <div role="alert" className="text-destructive">Unable to load cars.</div>;
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Cars</h1>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" />Add car
        </Button>
      </div>

      {cars && cars.length === 0 ? (
        <div className="text-center text-muted-foreground py-12 space-y-2">
          <p className="font-medium">No cars added</p>
          <p className="text-sm">
            Add a car to get started — you'll be able to record your setup and use it for event entries.
          </p>
          <Button onClick={openCreate} className="mt-4">Add your first car</Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {cars!.map(c => (
            <CarCard key={c.id} car={c} onClick={() => openEdit(c)} />
          ))}
        </div>
      )}

      <CarEditSheet open={open} onOpenChange={setOpen} car={selected} />
    </div>
  );
}
