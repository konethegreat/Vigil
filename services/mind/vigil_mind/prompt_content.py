# Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-28 — Serialized untrusted model input
from xml.etree.ElementTree import Element, tostring


def untrusted_element(tag: str, text: str, **attributes: str) -> str:
    """Serialize a caller-owned tag; source text can never create sibling instructions."""
    node = Element(tag, attributes)
    node.text = text
    return tostring(node, encoding="unicode", short_empty_elements=False)
