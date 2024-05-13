import requests
import shutil

def load_tile(z, x, y):
    request_url = f'http://localhost:8080/tile/{z}/{x}/{y}.png'
    response = requests.get(request_url, stream=True)
    path = f'./images/{z}_{x}_{y}.png'
    with (open(path, 'wb') as f):
        response.raw.decode_content = True
        shutil.copyfileobj(response.raw, f)

def load_contained_tiles(zoom, xtile, ytile):
    max_zoom = 19
    load_tile(zoom, xtile, ytile)
    if zoom == max_zoom:
        return
    next_top_leftx = 2*xtile
    next_top_lefty = 2*ytile
    new_zoom = zoom + 1
    offsets = [(0, 0), (0, 1), (1, 0), (1, 1)]
    for dx, dy in offsets:
        load_contained_tiles(new_zoom, next_top_leftx + dx, next_top_lefty+ dy)


# Load all Waterloo tiles at all zoom levels and store in images directory as png
if __name__ == '__main__':
    initial_zoom = 13
    xtile = 2263
    ytile = 2995
    load_contained_tiles(initial_zoom, xtile, ytile)