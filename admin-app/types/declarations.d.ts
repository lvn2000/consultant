// Temporary module declarations to silence missing-type errors for optional integrations.
// Replace or remove once proper types are available or unused dependencies removed.

declare module 'cloudflare:workers';
declare module '@deno/kv';
declare module '@scalar/api-reference';
declare module 'unctx/index';
declare module 'magic-string';
// 'jiti' resolves to a non-module entity in some builds; avoid declaring it here.
declare module 'crossws';
declare module 'crossws/adapters/node';
declare module '@farmfe/core';
declare module '@rspack/core';
declare module 'rolldown';
declare module 'unloader';
declare module '@capacitor/preferences';
declare module '@vercel/functions';
declare module 'db0';
declare module '@electric-sql/pglite';
declare module '@uploadthing/shared';
declare module 'unimport';
declare module 'unplugin';
declare module 'listhen';
declare module '@deno/kv';

// Common globals used by some libs
declare namespace Deno {
  type Kv = any;
}
declare type KVNamespace<T = any> = any;
declare type R2Bucket = any;

// Emscripten related placeholder
// Emscripten placeholders used by @electric-sql/pglite types
declare namespace Emscripten {
  type FileSystemType = any;
}
declare interface EmscriptenModule { [key: string]: any }

// Some libs reference `FS` as both a value and a type
declare var FS: any;
declare type FS = any;

// MagicString placeholder if used as a type/value
declare class MagicString {
  toString(): string;
}

export {};
