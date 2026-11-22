// Builds docs/report/ResolveIT_Report.html from docs/report/*.md (cover page, table of contents, diagrams).
// Then: msedge --headless --print-to-pdf=docs/report/ResolveIT_Report.pdf docs/report/ResolveIT_Report.html
import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const dir = join(root, 'docs', 'report');
const files = readdirSync(dir).filter((f) => /^\d\d-.*\.md$/.test(f)).sort();
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const inline = (s) => esc(s)
  .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_, alt, src) => `<figure><img src="${src}" alt="${alt}"><figcaption>${alt}</figcaption></figure>`)
  .replace(/`([^`]+)`/g, '<code>$1</code>')
  .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

function convert(md) {
  const lines = md.replace(/\r/g, '').split('\n');
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (/^```/.test(line)) { const code = []; i++; while (i < lines.length && !/^```/.test(lines[i])) code.push(esc(lines[i++])); i++; out.push(`<pre>${code.join('\n')}</pre>`); continue; }
    const h = line.match(/^(#{1,4})\s+(.*)/);
    if (h) { const level = h[1].length; const id = h[2].toLowerCase().replace(/[^a-z0-9]+/g, '-'); out.push(`<h${level} id="${id}">${inline(h[2])}</h${level}>`); i++; continue; }
    if (/^\|/.test(line)) {
      const rows = []; while (i < lines.length && /^\|/.test(lines[i])) rows.push(lines[i++]);
      const cells = (r) => r.replace(/^\||\|$/g, '').split(/(?<!\\)\|/).map((c) => inline(c.trim().replace(/\\\|/g, '|')));
      const body = rows.filter((r, n) => n !== 1);
      out.push(`<table><thead><tr>${cells(body[0]).map((c) => `<th>${c}</th>`).join('')}</tr></thead><tbody>${body.slice(1).map((r) => `<tr>${cells(r).map((c) => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody></table>`);
      continue;
    }
    if (/^\s*[-*]\s+/.test(line) || /^\s*\d+\.\s+/.test(line)) {
      const ordered = /^\s*\d+\./.test(line); const items = [];
      while (i < lines.length && (/^\s*[-*]\s+/.test(lines[i]) || /^\s*\d+\.\s+/.test(lines[i]))) items.push(inline(lines[i++].replace(/^\s*([-*]|\d+\.)\s+/, '')));
      out.push(`<${ordered ? 'ol' : 'ul'}>${items.map((x) => `<li>${x}</li>`).join('')}</${ordered ? 'ol' : 'ul'}>`);
      continue;
    }
    if (!line.trim()) { i++; continue; }
    const para = []; while (i < lines.length && lines[i].trim() && !/^(#|\||```|\s*[-*]\s|\s*\d+\.\s)/.test(lines[i])) para.push(lines[i++]);
    const html = inline(para.join(' '));
    out.push(html.startsWith('<figure>') ? html : `<p>${html}</p>`);
  }
  return out.join('\n');
}

const sections = files.map((f) => ({ title: readFileSync(join(dir, f), 'utf8').split('\n')[0].replace(/^#\s+/, ''), html: convert(readFileSync(join(dir, f), 'utf8')) }));
const toc = sections.map((s) => `<li><a href="#${s.title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}">${esc(s.title)}</a></li>`).join('');
const html = `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>ResolveIT Project Report</title><style>
@page { size: A4; margin: 18mm 16mm; }
body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 11pt; line-height: 1.5; color: #1f2433; }
.cover { height: 250mm; display: flex; flex-direction: column; justify-content: center; page-break-after: always; }
.cover h1 { font-size: 34pt; margin: 0; } .cover h2 { font-weight: 400; color: #4a5163; margin: 8px 0 40px; }
.cover table { width: 60%; } .toc { page-break-after: always; } .toc li { margin: 4px 0; }
section { page-break-before: always; } h1 { font-size: 20pt; border-bottom: 2px solid #3b5bdb; padding-bottom: 4px; } h2 { font-size: 14pt; margin-top: 18px; }
table { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 9pt; page-break-inside: auto; } tr { page-break-inside: avoid; }
th, td { border: 1px solid #ccd1dc; padding: 4px 6px; text-align: left; vertical-align: top; } th { background: #eef1f8; }
code { font-family: Consolas, monospace; font-size: 9pt; background: #f1f3f7; padding: 0 3px; } pre { background: #f6f7fa; padding: 8px; font-size: 9pt; white-space: pre-wrap; }
figure { margin: 12px 0; page-break-inside: avoid; text-align: center; } img { max-width: 100%; max-height: 230mm; } figcaption { font-size: 9pt; color: #5f6677; }
</style></head><body>
<div class="cover"><h1>ResolveIT</h1><h2>Intelligent IT Service Management and Incident Resolution System</h2>
<p>Object-Oriented Programming project report, Semester 1, 2026-2027</p>
<table><tr><th>Group number</th><td>&nbsp;</td></tr><tr><th>Members</th><td>&nbsp;<br>&nbsp;<br>&nbsp;<br>&nbsp;<br>&nbsp;</td></tr><tr><th>Repository</th><td>&nbsp;</td></tr><tr><th>Date</th><td>22 November 2026</td></tr></table></div>
<div class="toc"><h1>Contents</h1><ul style="list-style:none;padding-left:0">${toc}</ul></div>
${sections.map((s) => `<section>${s.html}</section>`).join('\n')}
</body></html>`;
writeFileSync(join(dir, 'ResolveIT_Report.html'), html);
console.log(`Wrote ${join(dir, 'ResolveIT_Report.html')} with ${sections.length} sections`);
