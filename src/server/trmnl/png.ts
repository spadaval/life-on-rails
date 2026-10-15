// A 2-bit grayscale PNG: the four grays the TRMNL X draws without dithering
// (0 black, 1 #555, 2 #aaa, 3 white). Its firmware decodes this depth natively.
import { crc32, deflateSync } from "node:zlib";

function chunk(type: string, data: Buffer): Buffer {
  const head = Buffer.alloc(8);
  head.writeUInt32BE(data.length, 0);
  head.write(type, 4, "ascii");
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([head.subarray(4), data])), 0);
  return Buffer.concat([head, data, crc]);
}

/** levels: one byte per pixel, 0-3, row by row. */
export function grayPng2(width: number, height: number, levels: Uint8Array): Buffer {
  if (levels.length !== width * height) throw new Error(`expected ${width * height} pixels, got ${levels.length}`);
  const stride = Math.ceil(width / 4);
  // Each row is a filter byte (0, none) and four pixels to a byte, leftmost in the high bits.
  const raw = Buffer.alloc((stride + 1) * height);
  for (let y = 0; y < height; y++) {
    const o = y * (stride + 1) + 1;
    for (let x = 0; x < width; x++) raw[o + (x >> 2)]! |= (levels[y * width + x]! & 3) << (6 - 2 * (x & 3));
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 2; // bit depth
  ihdr[9] = 0; // grayscale
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", ihdr),
    chunk("IDAT", deflateSync(raw, { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}
