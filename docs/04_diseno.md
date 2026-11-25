# Design — SkillBridge AI

A locked design system for this multi-page application. The visual language is inspired by Tally's approachable simplicity without copying its product, illustrations, or page composition.

## Genre
Modern-minimal, warm, direct, and quietly playful.

## Macrostructure family
- Marketing pages: Split Studio with a compact editorial product story.
- App pages: Workbench with a persistent side rail and dense, calm content canvas.
- Content and form pages: Long Document with narrow reading/form measures.

## Theme
- `--color-paper`: oklch(98% 0.006 300)
- `--color-paper-2`: oklch(96% 0.012 300)
- `--color-paper-3`: oklch(93% 0.018 300)
- `--color-ink`: oklch(20% 0.025 292)
- `--color-ink-2`: oklch(34% 0.028 292)
- `--color-rule`: oklch(88% 0.018 300)
- `--color-accent`: oklch(57% 0.22 296)
- `--color-focus`: oklch(62% 0.24 296)

## Typography
- Display: Manrope, weight 700–800, roman.
- Body: Inter, weight 400–700.
- Mono: Cascadia Mono, weight 500.
- Display tracking: -0.04em.
- Type scale anchor: `--text-display: clamp(2.75rem, 6vw, 5.5rem)`.

## Spacing
A four-point named scale lives in `tokens.css`. Components consume named tokens instead of raw spacing values.

## Motion
- Transform and opacity only.
- Brief hover and entrance transitions using `--ease-out`.
- Reduced-motion fallback removes spatial movement and keeps state changes under 150 ms.

## Microinteractions stance
- Silent success and inline feedback.
- No decorative toasts or bounce effects.
- Focus is immediate and visible.

## CTA voice
- Primary: violet solid, compact rounded rectangle, verb-led copy.
- Secondary: paper surface with a defined hairline border.

## Per-page allowances
- Marketing pages may use restrained CSS diagrams.
- App pages use functional information as the visual material.
- Form pages prioritize a single readable task flow.

## What pages MUST share
- SkillBridge AI wordmark.
- Violet accent and restrained use of soft violet surfaces.
- Manrope and Inter typography.
- Button, field, card, table, badge, and focus behavior.
- Side rail and topbar rhythm across authenticated roles.

## What pages MAY differ on
- Dashboard density and metric count.
- Table, card, or form composition according to the task.
- Public pages may use larger display typography.

## Exports

### tokens.css
The canonical CSS export is stored in `/tokens.css`.

### Tailwind v4 `@theme`
```css
@theme {
  --color-paper: oklch(98% 0.006 300);
  --color-ink: oklch(20% 0.025 292);
  --color-accent: oklch(57% 0.22 296);
  --font-display: "Manrope", "Inter", sans-serif;
  --font-body: "Inter", sans-serif;
  --spacing-md: 1.5rem;
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
}
```

### DTCG
The portable DTCG export is stored in `/tokens.json`.

### shadcn/ui CSS variables
```css
:root {
  --background: 98% 0.006 300;
  --foreground: 20% 0.025 292;
  --primary: 57% 0.22 296;
  --primary-foreground: 99% 0.004 300;
  --muted: 93% 0.018 300;
  --muted-foreground: 50% 0.025 292;
  --border: 88% 0.018 300;
  --input: 88% 0.018 300;
  --ring: 62% 0.24 296;
  --radius: 0.75rem;
}
```
