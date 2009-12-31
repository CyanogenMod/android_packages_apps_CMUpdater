package cmupdaterapp.customTypes;

public class InvalidPictureException extends Exception
{
	private static final long serialVersionUID = 1882894870160464818L;
	
	public InvalidPictureException() {}
	
	public InvalidPictureException(String msg)
	{
		super(msg);
	}
}