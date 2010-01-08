package cmupdaterapp.customTypes;

public class NotEnoughSpaceException extends Exception
{
	private static final long serialVersionUID = 658447306729869141L;

	public NotEnoughSpaceException() {}
	
	public NotEnoughSpaceException(String msg)
	{
		super(msg);
	}
}