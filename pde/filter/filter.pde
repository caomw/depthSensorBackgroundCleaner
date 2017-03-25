PImage colorMap;
PImage idx;
PImage edge;
PImage output;
PImage debug;

int w, h;

void setup()
{
  colorMap = loadImage("colorMap.png");
  idx = loadImage("playerIndex.png");
  edge = loadImage("edge.png");
  output = idx.get();
  w = colorMap.width;
  h = colorMap.height;
  debug = createImage(w, h, RGB);

  size(w*2, h*2);
}

boolean isValid(int index)
{
  return red(idx.pixels[index]) == 0xff;
}

void setValid(int index)
{
  idx.pixels[index] = color(255, 255, 255);
}

void draw()
{
  idx = loadImage("playerIndex.png");

  background(0);
  image(idx, 0, 0);

  int thresh = 30;//mouseX * 255/ width;

  for (int x=1; x<w-1; x++)
  {
    for (int y=h-1; y>1; y--)
    {
      int index = y*w+x;
      output.pixels[index] = 0xffffff;

      if (isValid(index))
      {
        // down->up
        color clr = edge.pixels[index-w];
        if (red(clr) +green(clr)+blue(clr) < thresh)
        {
          setValid(index-w);
        }

        // left->right
        clr = edge.pixels[index+1];
        if (red(clr) +green(clr)+blue(clr) < thresh)
        {
          setValid(index+1);
        }
      }
    }
  }

  // right->left
  for (int x=w-1; x>1; x--)
  {
    for (int y=h-1; y>1; y--)
    {
      int index = y*w+x;
      output.pixels[index] = 0xffffff;

      if (isValid(index))
      {
        color clr = edge.pixels[index-1];
        if (red(clr) +green(clr)+blue(clr) < thresh)
        {
          setValid(index-1);
        }
      }
    }
  }

  for (int x=0; x<w; x++)
  {
    for (int y=0; y<h; y++)
    {
      int index = y*w+x;
      if (isValid(index))
      {
        output.pixels[index] = color(0, 255, 0);
      } else
      {
        color clr = edge.pixels[index];
        if (red(clr) +green(clr)+blue(clr) > thresh)
        {
          output.pixels[index] = color(255, 0, 0);
        }
      }
    }
  }

  output.updatePixels();
  image(output, w, 0);

  debug.updatePixels();
  image(debug, w, h);
}
