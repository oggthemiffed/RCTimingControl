const LINK_CLASS = 'block text-sm text-primary hover:underline';

/** Links at the foot of a help panel to the printable guides that cover the same screen. */
export function GuideLinks({ admin = false, meeting = false }: { admin?: boolean; meeting?: boolean }) {
  return (
    <div className="mt-4 pt-4 border-t space-y-1">
      {admin && (
        <a href="/print/admin-guide" target="_blank" rel="noopener noreferrer" className={LINK_CLASS}>
          Open Admin Configuration Guide (printable)
        </a>
      )}
      {meeting && (
        <a href="/print/meeting-guide" target="_blank" rel="noopener noreferrer" className={LINK_CLASS}>
          Open Race Meeting Guide (printable)
        </a>
      )}
    </div>
  );
}
