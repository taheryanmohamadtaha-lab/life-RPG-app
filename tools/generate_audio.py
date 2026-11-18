#!/usr/bin/env python3
"""Synthesises every sound used by Life RPG from scratch (pure sine/bell synthesis).
All audio is original and released under the project licence (MIT).
Usage: python3 tools/generate_audio.py   (writes app/src/main/res/raw/*.wav)"""
import os, wave
import numpy as np

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
os.makedirs(OUT, exist_ok=True)

def note(freq, dur, vol=0.5, attack=0.01, decay=None, harmonics=((1, 1.0), (2, 0.35), (3, 0.12))):
    n = int(SR * dur)
    t = np.arange(n) / SR
    sig = np.zeros(n)
    for mult, amp in harmonics:
        sig += amp * np.sin(2 * np.pi * freq * mult * t)
    sig /= sum(a for _, a in harmonics)
    decay = decay if decay is not None else dur / 3.0
    env = np.minimum(1.0, t / max(attack, 1e-4)) * np.exp(-t / decay)
    return vol * sig * env

def mix(parts, total):
    out = np.zeros(int(SR * total))
    for start, sig in parts:
        i = int(SR * start)
        j = min(len(out), i + len(sig))
        out[i:j] += sig[: j - i]
    return out

def save(name, sig, peak=0.85):
    m = np.max(np.abs(sig)) or 1.0
    sig = sig / m * peak
    fade = int(SR * 0.004)
    sig[:fade] *= np.linspace(0, 1, fade)
    sig[-fade:] *= np.linspace(1, 0, fade)
    data = (sig * 32767).astype("<i2")
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data.tobytes())
    print(name, f"{len(sig)/SR:.2f}s", os.path.getsize(os.path.join(OUT, name + '.wav')) // 1024, "KB")

C5, D5, E5, G5, A5, C6, E6, G6 = 523.25, 587.33, 659.25, 783.99, 880.0, 1046.5, 1318.5, 1568.0

save("sfx_click", note(880, 0.07, attack=0.002, decay=0.02))
save("sfx_coin", mix([(0, note(1318.5, 0.25, decay=0.07)), (0.07, note(1760, 0.4, decay=0.12))], 0.5))
save("sfx_complete", mix([(0.00, note(C5, 0.35, decay=0.10)), (0.09, note(E5, 0.35, decay=0.10)),
                          (0.18, note(G5, 0.5, decay=0.16)), (0.18, note(C6, 0.5, decay=0.18, vol=0.4))], 0.8))
save("sfx_levelup", mix([(0.00, note(C5, 0.4, decay=0.12)), (0.12, note(E5, 0.4, decay=0.12)),
                         (0.24, note(G5, 0.4, decay=0.12)), (0.36, note(C6, 0.5, decay=0.15)),
                         (0.52, note(E6, 0.5, decay=0.15)), (0.68, note(G6, 1.2, decay=0.45)),
                         (0.68, note(C6, 1.2, decay=0.45, vol=0.5))], 2.0))
save("sfx_achievement", mix([(0.00, note(G5, 0.5, decay=0.18)), (0.15, note(C6, 0.5, decay=0.18)),
                             (0.30, note(E6, 0.9, decay=0.35)), (0.30, note(G6, 0.9, decay=0.3, vol=0.4)),
                             (0.55, note(A5 * 2, 1.0, decay=0.4, vol=0.35))], 1.8))

# Background music: slow A-minor pentatonic pad + soft bell arpeggio, 4 chords x 6 s = 24 s loop.
chords = [(220.0, 261.63, 329.63), (174.61, 220.0, 261.63), (130.81, 196.0, 261.63), (196.0, 246.94, 293.66)]
bars = 6.0
total = bars * len(chords)
pad = np.zeros(int(SR * total))
bell = np.zeros_like(pad)
pent = [440.0, 523.25, 587.33, 659.25, 783.99, 880.0]
rng = np.random.default_rng(7)
for ci, chord in enumerate(chords):
    n = int(SR * (bars + 1.0))
    t = np.arange(n) / SR
    env = np.minimum(1, t / 2.0) * np.minimum(1, (bars + 1.0 - t) / 2.0)
    seg = sum(np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 2 * t + 0.4) for f in chord) * env * 0.12
    i = int(SR * ci * bars)
    j = min(len(pad), i + n)
    pad[i:j] += seg[: j - i]
    # wrap the tail of the last chord to the start so the loop is seamless
    if i + n > len(pad):
        extra = seg[len(pad) - i:]
        pad[: len(extra)] += extra
for k in range(int(total / 1.5)):
    f = pent[rng.integers(0, len(pent))] * (0.5 if rng.random() < 0.3 else 1.0)
    s = note(f, 1.6, vol=0.10, attack=0.005, decay=0.5, harmonics=((1, 1.0), (2.76, 0.25), (5.4, 0.1)))
    i = int(SR * k * 1.5)
    j = min(len(bell), i + len(s))
    bell[i:j] += s[: j - i]
music = pad + bell
save("bgm_loop", music, peak=0.55)
