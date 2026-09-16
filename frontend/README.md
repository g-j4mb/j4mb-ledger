# j4mb-erp-frontend

A React SPA for the [j4mb-ledger](../README.md) backend — chart of accounts,
operational accounts, currencies/exchange rates, fiscal years/periods, and
journal entries, behind a role-based sign-in.

See the parent repo's [Frontend section](../README.md#frontend) for
screenshots, how to run it against the real backend, and a list of known
gaps between what this UI expects and what the backend currently exposes.

## Tech Stack

- React 19, TypeScript, Vite
- React Router
- TanStack Query for server state
- Zustand for client state
- Tailwind CSS + Radix UI primitives
- react-hook-form + Zod for forms/validation
- Mock Service Worker (MSW) for frontend-only development

## Development

```bash
npm install
npm run dev
```

By default (`VITE_USE_MOCK=true` in `.env`) the app runs entirely against an
in-browser mock API (`src/**/mock/`) — no backend required. Set
`VITE_USE_MOCK=false` and `VITE_API_BASE_URL` to run against a real
`j4mb-ledger` instance instead (see the parent README for what that instance
needs configured — CORS and a provisioned tenant).

## Project Structure

Feature-first, one folder per domain module:

```
src/
├── modules/
│   ├── accounts/        api, hooks, pages, mock fixtures, types
│   ├── coa/
│   ├── currencies/
│   ├── dashboard/
│   ├── fiscal/
│   ├── journals/
│   ├── postingRules/
│   ├── closing/
│   └── auth/
└── shell/                app-wide layout, routing, auth context, API client, UI primitives
```

Each module owns its own `types.ts`, matched to the corresponding backend
response DTOs — see the parent README's "Known gaps" section for where those
didn't originally line up and had to be corrected.

## Building

```bash
npm run build     # tsc -b && vite build
npm run lint
```
