// template routine Java
package routines;

import java.io.Serializable;

/*
 * user specification: the function's comment should contain keys as follows: 1. write about the function's comment.but
 * it must be before the "{talendTypes}" key.
 * 
 * 2. {talendTypes} 's value must be talend Type, it is required . its value should be one of: String, char | Character,
 * long | Long, int | Integer, boolean | Boolean, byte | Byte, Date, double | Double, float | Float, Object, short |
 * Short
 * 
 * 3. {Category} define a category for the Function. it is required. its value is user-defined .
 * 
 * 4. {param} 's format is: {param} <type>[(<default value or closed list values>)] <name>[ : <comment>]
 * 
 * <type> 's value should be one of: string, int, list, double, object, boolean, long, char, date. <name>'s value is the
 * Function's parameter name. the {param} is optional. so if you the Function without the parameters. the {param} don't
 * added. you can have many parameters for the Function.
 * 
 * 5. {example} gives a example for the Function. it is optional.
 */
public class routine_tMap_10_persistence_01_types {

	public static class CustomObject implements Serializable {

		public int index = -1;
		
		public CustomObject(int index) {
			super();
			this.index = index;
		}

		@Override
		public String toString() {
			return "CustomObject_instance " + index;
		}
		
		public boolean equals(Object other) {
			return this.index == ((CustomObject)other).index;
		}
		
	}
	
	public static CustomObject getCustomObject(int index) {
		return new CustomObject(index);
	}
	
}
