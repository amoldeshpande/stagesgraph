const fs = require('fs');
const bytes = fs.readFileSync(process.argv[2]);
for (const expected of ['STAGES GRAPHS  \u2022  LIVE BLUETOOTH', 'STAGES GRAPHS  \u2022  DEMO \u2014 SIMULATED DATA']) {
    if (!bytes.includes(Buffer.from(expected, 'utf8'))) {
        throw new Error('Compiled text is not UTF-8: ' + expected);
    }
}
console.log('Compiled Unicode text checks passed.');
