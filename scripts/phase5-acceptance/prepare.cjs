const fs = require('node:fs');
const { execFileSync } = require('node:child_process');
const path = require('node:path');
const root = process.cwd();
const artifactDir = path.join(root, '.codex-report-review/phase5');
fs.mkdirSync(artifactDir, { recursive:true });
// This directory contains resolved local credentials: exclude it before writing.
const exclude=path.join(root,'.git/info/exclude');
if (!fs.readFileSync(exclude,'utf8').includes('/.codex-report-review/')) fs.appendFileSync(exclude,'\n/.codex-report-review/\n');
const cfg = JSON.parse(execFileSync('docker', ['compose','config','--format','json'], { encoding:'utf8' }));
cfg.name = 'lms-phase5-acceptance';
for (const [name, svc] of Object.entries(cfg.services)) {
    if (svc.build) { svc.image = `lms-main-${name}:latest`; delete svc.build; }
    svc.container_name = `lms-phase5-acceptance-${name}`;
    if (svc.environment && svc.environment.SERVER_PORT) svc.environment.JAVA_TOOL_OPTIONS = '-Xmx256m';
}
for (const [name, volume] of Object.entries(cfg.volumes)) volume.name = `lms-phase5-acceptance_${name}`;
for (const [name, network] of Object.entries(cfg.networks)) network.name = `lms-phase5-acceptance_${name}`;
cfg.services['identity-service'].environment.BOOTSTRAP_ADMIN_ENABLED = 'true';
fs.writeFileSync(path.join(artifactDir,'compose.json'), JSON.stringify(cfg,null,2));
console.log('Prepared isolated Compose: separate containers/network/volumes, same ports; configuration kept locally excluded from Git.');
