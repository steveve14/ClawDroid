import os
from PIL import Image, ImageDraw

def create_icon(size, path, filename):
    img = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    draw.ellipse([0, 0, size - 1, size - 1], fill='#5B21B6')
    
    if not os.path.exists(path):
        os.makedirs(path)
    
    img.save(os.path.join(path, filename))
    print(f"Created: {os.path.join(path, filename)}")

base_res_path = r'c:\Users\User\WorkStation\ClawDroid\app\src\main\res'
sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

for folder, size in sizes.items():
    folder_path = os.path.join(base_res_path, folder)
    create_icon(size, folder_path, 'ic_launcher.png')
    create_icon(size, folder_path, 'ic_launcher_round.png')
