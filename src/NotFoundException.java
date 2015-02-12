

public class NotFoundException extends WebServerException {

	private static final long serialVersionUID = -6841103239932981316L;
	
	@Override
	public String getMessage(){
		return ConfigUtil.NOT_FOUND;
	}

}
