// Semantic-release plugin: makes the release notes from the commits, like
// @semantic-release/release-notes-generator (and with its options), unless
// .github/release-notes/v<version>.md exists. Then that file's hand-written notes
// are used under the generated heading instead of the list of commits.
import { readFile } from "node:fs/promises";
import path from "node:path";
import { generateNotes as generateFromCommits } from "@semantic-release/release-notes-generator";

export async function generateNotes(pluginConfig, context) {
  const notes = await generateFromCommits(pluginConfig, context);
  const file = path.join(context.cwd, ".github", "release-notes", `v${context.nextRelease.version}.md`);
  let written;
  try {
    written = await readFile(file, "utf8");
  } catch {
    return notes;
  }
  context.logger.log("Using the release notes in %s", file);
  const heading = notes.split("\n")[0];
  return `${heading}\n\n${written.replace(/\r\n/g, "\n").trim()}\n`;
}
