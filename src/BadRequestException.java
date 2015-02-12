

public class BadRequestException extends WebServerException {


	private static final long serialVersionUID = -6064391674548317274L;
	
	
	@Override
	public String getMessage(){
		return ConfigUtil.BAD_REQUEST;
		
	}
	
}
