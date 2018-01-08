/*
 *	Kernel library for xapps.
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			december, 2nd 2010
 */
 
#unit UTILS

external abstract class Math
{
	static readonly float PI;
	
	static float sin(float x);
	static float cos(float x);
	static float asin(float x);
	static float acos(float x);
	static float atan(float x);
	static float pow(float base, float exp);
	static float exp(float x);
	
	static float min(float x, float y);
	static int min(int x, int y);
	static float max(float x, float y);
	static int max(int x, int y);
	static float abs(float x);
	static int abs(int x);
	
	static int ceil(float x);		// ceiling (round up)
	static int floor(float x);		// floor (round down)
	static int round(float x);		// round to nearest
	
	static float rand(float min, float max);		// random number
}

external class Color
{
	// some predefined colors
	static readonly object<Color> transparent;
	static readonly object<Color> black;
	static readonly object<Color> white;
	static readonly object<Color> gray25;
	static readonly object<Color> gray50;
	static readonly object<Color> gray75;
	static readonly object<Color> red;
	static readonly object<Color> green;
	static readonly object<Color> blue;
	static readonly object<Color> yellow;
	static readonly object<Color> cyan;
	static readonly object<Color> magenta;
	static readonly object<Color> orange;
	
	static object<Color> fromHSB(float h, float s, float b);
	
	object<Color>(float r, float g, float b, float a);
}