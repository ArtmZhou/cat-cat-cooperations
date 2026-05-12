# Issue tracker: GitHub + local markdown

Issues and PRDs for this repo live primarily as GitHub issues. Use the `gh` CLI for all operations. Local markdown files under `.scratch/<feature>/` are used for quick or solo items that don't need the full GitHub workflow.

## GitHub (primary)

### Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --comments`, filtering comments by `jq` and also fetching labels.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

Infer the repo from `git remote -v` — `gh` does this automatically when run inside a clone.

### When a skill says "publish to the issue tracker"

Create a GitHub issue.

### When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.

## Local markdown (secondary)

For quick or solo items, create a markdown file under `.scratch/<feature-name>/issue.md`.

### Convention

```
.scratch/<feature-name>/
├── issue.md       # Issue body (title is the feature-name slug)
└── comments/      # Optional threaded discussion as separate .md files
```

- **Create**: write `.scratch/<feature-name>/issue.md` with the issue body.
- **Read**: read `.scratch/<feature-name>/issue.md` and any files under `comments/`.
- **List**: glob `.scratch/*/issue.md`.

### When to use each

- **GitHub Issues** for anything that needs collaboration, PR linkage, or formal tracking.
- **Local markdown** for personal notes, quick TODOs, or experimental features before they're ready for the shared tracker.
