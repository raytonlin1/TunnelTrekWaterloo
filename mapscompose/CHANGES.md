Original code at https://github.com/p-lr/MapCompose

1. Converted from Jetpack Compose to Compose Multiplatform lib
2. Replaced Android Graphics APIs w/ Compose Multiplatform graphics APIs
3. lots of ugly hacks (it's just a PoC, as there is no multiplatform maps renderer for compose)
4. added some desktop QoL improvements like scroll to zoom
5. fix missing markers at some zoom levels on desktop
   
   as it turns out, the layout container was being translated offscreen at some scroll positions
       when zoomed in, and the markers were out of the bounds of the container

   when the bounding box of the layout (in MarkerLayout)
       no longer intersects with the visible
       window area, the markers also disappear (probably some sort of render perf opt in compose/gl?)

   why this doesn't also happen on android is beyond me

   the fix was to apply x, y pos translatation manually while computing placeable positions

   the downside is that this may be much less efficient than the graphicsLayer translate modifiers. only time will tell
6. fix resource leak with TileStreamProvider usage. We need to close the InputStream.
