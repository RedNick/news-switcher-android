"""Draws the Google Play listing graphics: app icon, feature graphic and TV banner.

All three use the app's look: red background, a white TV with a clock on it
(the same mark as the launcher icon), and the app name.

Usage (needs Pillow and the DejaVu fonts):
    python3 play-store/make_graphics.py

Writes into play-store/graphics/.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

GRAPHICS_FOLDER = Path(__file__).parent / "graphics"
BOLD_FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
REGULAR_FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

BACKGROUND_RED = "#B71C1C"
WHITE = "#FFFFFF"
SOFT_PINK = "#FFCDD2"

# Draw everything this many times larger, then shrink, for smooth edges.
SUPERSAMPLE = 4


def draw_tv_and_clock(draw, left, top, size):
    """Draws the TV-with-a-clock mark in a square of the given size (pixels)."""

    def at(x, y):
        # Positions are worked out on a 100x100 grid, then scaled to the square.
        return (left + x * size / 100, top + y * size / 100)

    line_width = max(1, round(size * 0.06))
    draw.rounded_rectangle([at(4, 12), at(96, 82)], radius=size * 0.09, outline=WHITE, width=line_width)
    draw.rectangle([at(34, 88), at(66, 94)], fill=WHITE)
    draw.ellipse([at(28, 24), at(72, 68)], outline=WHITE, width=round(line_width * 0.85))
    hand_width = round(line_width * 0.85)
    draw.line([at(50, 46), at(50, 32)], fill=WHITE, width=hand_width)
    draw.line([at(50, 46), at(60, 52)], fill=WHITE, width=hand_width)


def draw_name_and_tagline(draw, left, top, title_size, tagline=True):
    title_font = ImageFont.truetype(BOLD_FONT_PATH, title_size)
    draw.text((left, top), "News", font=title_font, fill=WHITE)
    draw.text((left, top + title_size * 1.1), "Switcher", font=title_font, fill=WHITE)
    if tagline:
        tagline_font = ImageFont.truetype(REGULAR_FONT_PATH, round(title_size * 0.42))
        draw.text((left + title_size * 0.06, top + title_size * 2.45), "live news, every hour",
                  font=tagline_font, fill=SOFT_PINK)


def new_canvas(width, height):
    image = Image.new("RGB", (width * SUPERSAMPLE, height * SUPERSAMPLE), BACKGROUND_RED)
    return image, ImageDraw.Draw(image)


def save_shrunk(image, width, height, file_name):
    image.resize((width, height), Image.LANCZOS).save(GRAPHICS_FOLDER / file_name, optimize=True)


def make_icon():
    """512x512 app icon for the store: just the mark, like the launcher icon."""
    side = 512 * SUPERSAMPLE
    image, draw = new_canvas(512, 512)
    mark_size = side * 0.62
    draw_tv_and_clock(draw, (side - mark_size) / 2, (side - mark_size) / 2, mark_size)
    save_shrunk(image, 512, 512, "icon.png")


def make_wide_graphic(width, height, file_name):
    """A landscape graphic with the mark on the left and the name on the right."""
    image, draw = new_canvas(width, height)
    scaled_height = height * SUPERSAMPLE
    mark_size = scaled_height * 0.52
    mark_left = width * SUPERSAMPLE * 0.12
    draw_tv_and_clock(draw, mark_left, (scaled_height - mark_size) / 2, mark_size)
    title_size = round(scaled_height * 0.17)
    draw_name_and_tagline(draw, mark_left + mark_size * 1.25, scaled_height * 0.25, title_size)
    save_shrunk(image, width, height, file_name)


if __name__ == "__main__":
    GRAPHICS_FOLDER.mkdir(exist_ok=True)
    make_icon()
    make_wide_graphic(1024, 500, "feature-graphic.png")
    make_wide_graphic(1280, 720, "tv-banner.png")
