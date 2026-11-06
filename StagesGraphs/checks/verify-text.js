const fs = require('fs');
const path = require('path');
const directory = path.dirname(process.argv[2]);
const bytes = Buffer.concat(fs.readdirSync(directory).filter(name => name.endsWith('.class')).map(name => fs.readFileSync(path.join(directory,name))));
for (const expected of ['Connecting\u2026', '\u2014']) {
    if (!bytes.includes(Buffer.from(expected, 'utf8'))) {
        throw new Error('Compiled text is not UTF-8: ' + expected);
    }
}
console.log('Compiled Unicode text checks passed.');
