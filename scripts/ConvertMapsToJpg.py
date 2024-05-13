from PIL import Image
import os

def compress_maps():
    path = "./images"
    for file in os.listdir(path):
        if file.endswith(".png"):
            img = Image.open(file).convert('RGB')
            file_name, file_ext = os.path.splitext(file)
            img.save(f'./images/{file_name}.jpg')


if __name__ == '__main__':
    compress_maps()
