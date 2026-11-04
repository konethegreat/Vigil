// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Pure-Rust FIPS 180-4 SHA-256, RFC 2104 HMAC-SHA-256, and W4.6 psn_ pseudonym derivation (P3 tasks 3.2, 3.7)
//! Deterministic, dependency-free FIPS 180-4 SHA-256 and RFC 2104 HMAC-SHA-256 primitives
//! used for W12 audit hash-chaining, policy-context hashing (`pch`), and W4.6 `psn_`
//! pseudonym generation across native and WebAssembly targets.

const K: [u32; 64] = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
];

const H0: [u32; 8] = [
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
];

fn compress_block(state: &mut [u32; 8], block: &[u8; 64]) {
    let mut w = [0u32; 64];
    for (i, slot) in w.iter_mut().take(16).enumerate() {
        let j = i * 4;
        *slot = u32::from_be_bytes([block[j], block[j + 1], block[j + 2], block[j + 3]]);
    }
    for i in 16..64 {
        let s0 = w[i - 15].rotate_right(7) ^ w[i - 15].rotate_right(18) ^ (w[i - 15] >> 3);
        let s1 = w[i - 2].rotate_right(17) ^ w[i - 2].rotate_right(19) ^ (w[i - 2] >> 10);
        w[i] = w[i - 16]
            .wrapping_add(s0)
            .wrapping_add(w[i - 7])
            .wrapping_add(s1);
    }

    let [mut a, mut b, mut c, mut d, mut e, mut f, mut g, mut h] = *state;
    for i in 0..64 {
        let s1 = e.rotate_right(6) ^ e.rotate_right(11) ^ e.rotate_right(25);
        let ch = (e & f) ^ ((!e) & g);
        let temp1 = h
            .wrapping_add(s1)
            .wrapping_add(ch)
            .wrapping_add(K[i])
            .wrapping_add(w[i]);
        let s0 = a.rotate_right(2) ^ a.rotate_right(13) ^ a.rotate_right(22);
        let maj = (a & b) ^ (a & c) ^ (b & c);
        let temp2 = s0.wrapping_add(maj);

        h = g;
        g = f;
        f = e;
        e = d.wrapping_add(temp1);
        d = c;
        c = b;
        b = a;
        a = temp1.wrapping_add(temp2);
    }

    state[0] = state[0].wrapping_add(a);
    state[1] = state[1].wrapping_add(b);
    state[2] = state[2].wrapping_add(c);
    state[3] = state[3].wrapping_add(d);
    state[4] = state[4].wrapping_add(e);
    state[5] = state[5].wrapping_add(f);
    state[6] = state[6].wrapping_add(g);
    state[7] = state[7].wrapping_add(h);
}

/// Computes the 32-byte FIPS 180-4 SHA-256 digest of `data`.
#[must_use]
pub fn sha256(data: &[u8]) -> [u8; 32] {
    let mut state = H0;
    let mut chunks = data.chunks_exact(64);
    for chunk in &mut chunks {
        let mut block = [0u8; 64];
        block.copy_from_slice(chunk);
        compress_block(&mut state, &block);
    }
    let rem = chunks.remainder();
    let bit_len = (data.len() as u64).wrapping_mul(8);
    let mut pad = [0u8; 128];
    pad[..rem.len()].copy_from_slice(rem);
    pad[rem.len()] = 0x80;
    if rem.len() < 56 {
        pad[56..64].copy_from_slice(&bit_len.to_be_bytes());
        let mut b0 = [0u8; 64];
        b0.copy_from_slice(&pad[..64]);
        compress_block(&mut state, &b0);
    } else {
        pad[120..128].copy_from_slice(&bit_len.to_be_bytes());
        let mut b0 = [0u8; 64];
        let mut b1 = [0u8; 64];
        b0.copy_from_slice(&pad[..64]);
        b1.copy_from_slice(&pad[64..128]);
        compress_block(&mut state, &b0);
        compress_block(&mut state, &b1);
    }
    let mut out = [0u8; 32];
    for (i, word) in state.iter().enumerate() {
        out[i * 4..(i + 1) * 4].copy_from_slice(&word.to_be_bytes());
    }
    out
}

/// Computes the lowercase hexadecimal SHA-256 digest of `data`.
#[must_use]
pub fn sha256_hex(data: &[u8]) -> String {
    hex_encode(&sha256(data))
}

/// Encodes `bytes` as lowercase hexadecimal.
#[must_use]
pub fn hex_encode(bytes: &[u8]) -> String {
    const HEX: &[u8; 16] = b"0123456789abcdef";
    let mut out = String::with_capacity(bytes.len() * 2);
    for &b in bytes {
        out.push(HEX[(b >> 4) as usize] as char);
        out.push(HEX[(b & 0x0f) as usize] as char);
    }
    out
}

/// Computes the 32-byte RFC 2104 HMAC-SHA-256 over `message` under `key`.
#[must_use]
pub fn hmac_sha256(key: &[u8], message: &[u8]) -> [u8; 32] {
    let mut key_block = [0u8; 64];
    if key.len() > 64 {
        let hashed = sha256(key);
        key_block[..32].copy_from_slice(&hashed);
    } else {
        key_block[..key.len()].copy_from_slice(key);
    }
    let mut ipad = [0x36u8; 64];
    let mut opad = [0x5cu8; 64];
    for i in 0..64 {
        ipad[i] ^= key_block[i];
        opad[i] ^= key_block[i];
    }
    let mut inner = Vec::with_capacity(64 + message.len());
    inner.extend_from_slice(&ipad);
    inner.extend_from_slice(message);
    let inner_hash = sha256(&inner);

    let mut outer = [0u8; 96];
    outer[..64].copy_from_slice(&opad);
    outer[64..96].copy_from_slice(&inner_hash);
    sha256(&outer)
}

/// Encodes `bytes` in lowercase RFC 4648 Base32 without padding.
#[must_use]
pub fn base32_lower(bytes: &[u8]) -> String {
    const ALPHABET: &[u8; 32] = b"abcdefghijklmnopqrstuvwxyz234567";
    let mut out = String::with_capacity((bytes.len() * 8).div_ceil(5));
    let mut buffer: u32 = 0;
    let mut bits_left: u32 = 0;
    for &b in bytes {
        buffer = (buffer << 8) | u32::from(b);
        bits_left += 8;
        while bits_left >= 5 {
            bits_left -= 5;
            let idx = ((buffer >> bits_left) & 0x1f) as usize;
            out.push(ALPHABET[idx] as char);
        }
    }
    if bits_left > 0 {
        let idx = ((buffer << (5 - bits_left)) & 0x1f) as usize;
        out.push(ALPHABET[idx] as char);
    }
    out
}

/// Derives a deterministic W4.6 pseudonym (`psn_` + 26 lowercase base32 chars of
/// `HMAC-SHA-256(tenant_key, kind || ":" || normalized_value)`).
#[must_use]
pub fn derive_pseudonym(tenant_key: &[u8], kind: &str, normalized_value: &str) -> String {
    let mut msg = Vec::with_capacity(kind.len() + 1 + normalized_value.len());
    msg.extend_from_slice(kind.as_bytes());
    msg.push(b':');
    msg.extend_from_slice(normalized_value.as_bytes());
    let mac = hmac_sha256(tenant_key, &msg);
    let b32 = base32_lower(&mac);
    format!("psn_{}", &b32[..26])
}

/// Derives a deterministic W2/section 4.6 Identifier primary key (`idf_` + 26 lowercase base32 chars).
#[must_use]
pub fn derive_identifier_key(tenant_key: &[u8], kind: &str, normalized_value: &str) -> String {
    let mut msg = Vec::with_capacity(kind.len() + 1 + normalized_value.len());
    msg.extend_from_slice(kind.as_bytes());
    msg.push(b':');
    msg.extend_from_slice(normalized_value.as_bytes());
    let mac = hmac_sha256(tenant_key, &msg);
    let b32 = base32_lower(&mac);
    format!("idf_{}", &b32[..26])
}

/// Masks a clear identifier string for `PII`-cleared display (`******4521`).
#[must_use]
pub fn mask_display(clear_value: &str) -> String {
    let chars: Vec<char> = clear_value.chars().collect();
    if chars.len() <= 4 {
        return "******".to_owned();
    }
    let suffix: String = chars[chars.len() - 4..].iter().collect();
    format!("******{suffix}")
}
