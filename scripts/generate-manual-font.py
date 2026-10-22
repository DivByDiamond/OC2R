"""Build the manual's ASCII atlas from the bundled OFL Monocraft font (Pillow)."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

resources = Path(__file__).resolve().parents[1] / "src/main/resources/assets/oc2r"
font = ImageFont.truetype(str(resources / "fonts/monocraft-r.ttf"), 8)
atlas = Image.new("RGBA", (96, 96))
for index, codepoint in enumerate(range(32, 127)):
    glyph = Image.new("RGBA", (6, 9))
    ImageDraw.Draw(glyph).text((0, 0), chr(codepoint), font=font, fill="white")
    atlas.paste(glyph, ((index % 16) * 6, (index // 16) * 9))

output = resources / "textures/font/monospace.png"
output.parent.mkdir(parents=True, exist_ok=True)
atlas.save(output)
