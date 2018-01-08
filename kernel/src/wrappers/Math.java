package wrappers;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

public abstract class Math extends IScriptable 
{
	public static final Double PI = java.lang.Math.PI;
	
	public static Double sin(Double x) {
		return java.lang.Math.sin(x);
	}	
	public static Double cos(Double x) {
		return java.lang.Math.cos(x);
	}
	public static Double asin(Double x) {
		return java.lang.Math.asin(x);
	}
	public static Double acos(Double x) {
		return java.lang.Math.acos(x);
	}
	public static Double atan(Double x) {
		return java.lang.Math.atan(x);
	}
	public static Double pow(Double base, Double exp) {
		return java.lang.Math.pow(base, exp);
	}
	public static Double exp(Double x) {
		return java.lang.Math.exp(x);
	}
	
	public static Double min(Double x, Double y) {
		return java.lang.Math.min(x, y);
	}
	public static Integer min(Integer x, Integer y) {
		return java.lang.Math.min(x, y);
	}
	public static Double max(Double x, Double y) {
		return java.lang.Math.max(x, y);
	}
	public static Integer max(Integer x, Integer y) {
		return java.lang.Math.max(x, y);
	}
	public static Double abs(Double x) {
		return java.lang.Math.abs(x);
	}
	public static Integer abs(Integer x) {
		return java.lang.Math.abs(x);
	}
	
	public static Integer ceil(Double x) {
		return Double.valueOf(java.lang.Math.ceil(x)).intValue();
	}
	public static Integer floor(Double x) {
		return Double.valueOf(java.lang.Math.floor(x)).intValue();
	}
	public static Integer round(Double x) {
		return Double.valueOf(java.lang.Math.round(x)).intValue();
	}
	
	public static Double rand(Double min, Double max) {
		return java.lang.Math.random() * (max-min) + min;
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
