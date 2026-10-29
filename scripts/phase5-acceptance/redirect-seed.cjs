// Redirect only Docker container names; the unchanged seed script writes to real isolated databases.
const cp = require('node:child_process');
const original = cp.execSync;
cp.execSync = function(command, options) {
    const mapped = command.replace(/\blms-mysql\b/g, 'lms-phase5-acceptance-mysql')
        .replace(/\blms-mongodb\b/g, 'lms-phase5-acceptance-mongodb');
    return original.call(this, mapped, options);
};
const originalFile = cp.execFileSync;
cp.execFileSync = function(file, args, options) {
    if (file === 'docker' && Array.isArray(args)) args=args.map(a=>a==='lms-mongodb'?'lms-phase5-acceptance-mongodb':a==='lms-mysql'?'lms-phase5-acceptance-mysql':a);
    return originalFile.call(this,file,args,options);
};
