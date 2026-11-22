// Builds docs/report/ResolveIT_Report.html from docs/report/NN-*.md (cover page, index, figures).
// Usage: node scripts/build-report.mjs [pages.json]   (pages.json maps heading text -> page number for the index)
// Then print to PDF, e.g. msedge --headless --no-pdf-header-footer --print-to-pdf=<pdf> <html>
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const dir = join(root, 'docs', 'report');
const pages = process.argv[2] && existsSync(process.argv[2]) ? JSON.parse(readFileSync(process.argv[2], 'utf8')) : {};
const files = readdirSync(dir).filter((f) => /^\d\d-.*\.md$/.test(f)).sort();
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const slug = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-');
const headings = [];

const inline = (s) => esc(s)
  .replace(/!\[([^\]]*)\]\(([^)]+)\)(\{\.shot\})?/g, (_, alt, src, shot) => `<figure class="${shot ? 'shot' : 'diagram'}"><img src="${src}" alt="${alt}"><figcaption>${alt}</figcaption></figure>`)
  .replace(/`([^`]+)`/g, '<code>$1</code>')
  .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  .replace(/(^|[\s(])\*([^*\s][^*]*)\*/g, '$1<em>$2</em>')
  .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

function convert(md) {
  const lines = md.replace(/\r/g, '').split('\n');
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (/^```/.test(line)) {
      const code = [];
      i++;
      while (i < lines.length && !/^```/.test(lines[i])) code.push(esc(lines[i++]));
      i++;
      out.push(`<pre>${code.join('\n')}</pre>`);
      continue;
    }
    const h = line.match(/^(#{1,3})\s+(.*)/);
    if (h) {
      const level = h[1].length;
      if (level <= 2) headings.push({ level, text: h[2] });
      out.push(`<h${level} id="${slug(h[2])}">${inline(h[2])}</h${level}>`);
      i++;
      continue;
    }
    if (/^>/.test(line)) {
      const quote = [];
      while (i < lines.length && /^>/.test(lines[i])) quote.push(lines[i++].replace(/^>\s?/, ''));
      out.push(`<blockquote>${inline(quote.join(' '))}</blockquote>`);
      continue;
    }
    if (/^\|/.test(line)) {
      const rows = [];
      while (i < lines.length && /^\|/.test(lines[i])) rows.push(lines[i++]);
      const cells = (r) => r.replace(/^\||\|$/g, '').split(/(?<!\\)\|/).map((c) => inline(c.trim().replace(/\\\|/g, '|')));
      const body = rows.filter((r, n) => n !== 1);
      out.push(`<table><thead><tr>${cells(body[0]).map((c) => `<th>${c}</th>`).join('')}</tr></thead><tbody>${body.slice(1).map((r) => `<tr>${cells(r).map((c) => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody></table>`);
      continue;
    }
    if (/^\s*[-*]\s+/.test(line) || /^\s*\d+\.\s+/.test(line)) {
      const ordered = /^\s*\d+\./.test(line);
      const items = [];
      while (i < lines.length && (/^\s*[-*]\s+/.test(lines[i]) || /^\s*\d+\.\s+/.test(lines[i]))) items.push(inline(lines[i++].replace(/^\s*([-*]|\d+\.)\s+/, '')));
      out.push(`<${ordered ? 'ol' : 'ul'}>${items.map((x) => `<li>${x}</li>`).join('')}</${ordered ? 'ol' : 'ul'}>`);
      continue;
    }
    if (!line.trim()) { i++; continue; }
    const para = [];
    while (i < lines.length && lines[i].trim() && !/^(#|\||```|>|\s*[-*]\s|\s*\d+\.\s)/.test(lines[i])) para.push(lines[i++]);
    const html = inline(para.join(' '));
    out.push(html.startsWith('<figure') ? html : `<p>${html}</p>`);
  }
  return out.join('\n');
}

const body = files.map((f) => convert(readFileSync(join(dir, f), 'utf8'))).join('\n');
const index = headings.map((h) => `<div class="toc-row toc-l${h.level}"><a href="#${slug(h.text)}">${esc(h.text)}</a><span class="dots"></span><span class="pg">${pages[h.text] ?? ''}</span></div>`).join('');

const team = [
  ['Shaurya Sharma', '2025A7PS0091H', 'Intelligent features and persistence (integration)'],
  ['Rish Jain', '2025A7PS0107H', 'Backend and APIs'],
  ['Jinay Shah', '2025A7PS0136H', 'Backend OOP architecture'],
  ['Heet Mehta', '2025A7PS0043H', 'UI / UX (frontend)'],
];

const html = `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>ResolveIT - Project Report</title><style>
@page { size: A4; margin: 20mm 18mm 18mm 18mm; @bottom-center { content: counter(page); font: 9pt Arial, sans-serif; color: #555; } }
@page :first { @bottom-center { content: none; } }
* { box-sizing: border-box; }
body { font-family: Arial, Helvetica, sans-serif; font-size: 10.5pt; line-height: 1.5; color: #111; margin: 0; }
.cover { height: 255mm; display: flex; flex-direction: column; justify-content: space-between; break-after: page; padding-top: 30mm; }
.cover .course { font-size: 11pt; letter-spacing: .5px; color: #444; text-transform: uppercase; }
.cover h1 { break-before: auto; font-size: 40pt; margin: 6mm 0 2mm; border: 0; letter-spacing: -1px; }
.cover .subtitle { font-size: 15pt; color: #333; margin: 0 0 4mm; }
.cover .rule { border-top: 2px solid #111; width: 60mm; margin: 6mm 0; }
.cover .label { font-size: 9pt; text-transform: uppercase; letter-spacing: 1px; color: #555; margin: 0 0 2mm; }
.cover table { width: 100%; font-size: 10.5pt; }
.cover .meta { font-size: 10pt; color: #333; }
.index { break-after: page; }
.index h1 { break-before: auto; }
.toc-row { display: flex; align-items: baseline; gap: 4px; padding: 2px 0; }
.toc-row a { color: #111; text-decoration: none; }
.toc-l1 { font-weight: bold; margin-top: 7px; }
.toc-l2 { padding-left: 7mm; font-size: 10pt; color: #333; }
.toc-row .dots { flex: 1; border-bottom: 1px dotted #999; transform: translateY(-3px); }
.toc-row .pg { min-width: 18px; text-align: right; }
h1 { font-size: 19pt; margin: 0 0 4mm; padding-bottom: 2mm; border-bottom: 1.5px solid #111; break-before: page; }
h2 { font-size: 13pt; margin: 6mm 0 2mm; break-after: avoid; }
h3 { font-size: 11pt; margin: 4mm 0 1mm; break-after: avoid; }
p { margin: 0 0 2.5mm; text-align: justify; }
ul, ol { margin: 0 0 3mm; padding-left: 6mm; } li { margin-bottom: 1mm; }
table { border-collapse: collapse; width: 100%; margin: 2mm 0 4mm; font-size: 9pt; break-inside: auto; }
tr { break-inside: avoid; }
th, td { border: 1px solid #999; padding: 3px 5px; text-align: left; vertical-align: top; }
th { background: #e6e6e6; font-weight: bold; }
.cover th, .cover td { border: 1px solid #777; padding: 5px 7px; } .cover th { background: #ececec; }
code { font-family: Consolas, 'Courier New', monospace; font-size: 8.8pt; background: #f0f0f0; padding: 0 2px; }
pre { font-family: Consolas, 'Courier New', monospace; font-size: 8.5pt; background: #f4f4f4; border: 1px solid #ccc; padding: 6px 8px; white-space: pre-wrap; break-inside: avoid; }
blockquote { margin: 3mm 0; padding: 2mm 4mm; border-left: 3px solid #555; background: #f4f4f4; font-style: italic; }
figure { margin: 4mm 0; text-align: center; break-inside: avoid; }
figure img { max-width: 100%; max-height: 215mm; border: 1px solid #ccc; }
figure.shot img { max-height: 105mm; filter: grayscale(100%); }
figcaption { font-size: 9pt; color: #444; margin-top: 1.5mm; }
a { color: #111; }
</style></head><body>
<div class="cover">
  <div>
    <p class="course">CS F213 / MAC F212 — Object-Oriented Programming · Semester I, 2026–2027</p>
    <h1>ResolveIT</h1>
    <p class="subtitle">Intelligent IT Service Management and Incident Resolution System</p>
    <p class="meta">Project Report</p>
    <div class="rule"></div>
  </div>
  <div>
    <p class="label">Team members</p>
    <table><thead><tr><th>Name</th><th>ID</th><th>Responsibility</th></tr></thead>
    <tbody>${team.map(([n, id, r]) => `<tr><td>${n}</td><td>${id}</td><td>${r}</td></tr>`).join('')}</tbody></table>
    <p class="meta" style="margin-top:6mm">Repository: https://github.com/jinayshah3210/Incident-Resolution-System<br>Date: 22 November 2026</p>
  </div>
</div>
<div class="index"><h1>Index</h1>${index}</div>
${body}
</body></html>`;
writeFileSync(join(dir, 'ResolveIT_Report.html'), html);
console.log(`Wrote ResolveIT_Report.html with ${headings.filter((h) => h.level === 1).length} chapters and ${headings.length} index entries`);
