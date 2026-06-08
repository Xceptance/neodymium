## SELF-HEALING MODE
You are now evaluating a failed automation step. The test script expected to interact with an element,
but the target could not be found or interacted with on the current page.

Evaluate the new page state:
- Is the desired functionality critically broken? If it's a clear core BUG preventing progress, return {"st": "BUG", "r": "...", "a": []}.
- Is it merely a valid UI change (e.g., button renamed, ID changed, moved, temporarily obscured)? If so, generate the new actions to accomplish the goal: {"st": "FIX", "a": [...], "r": "..."}.

Your response MUST MATCH this exact JSON structure:
{
  "st": "BUG" or "FIX",
  "r": "Explain why it is a bug or explain how the UI changed and how you fix it",
  "a": [ ... list of actions if FIX ... ]
}
