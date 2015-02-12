
public class InternalErrorException extends WebServerException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1470587954103626701L;

	@Override
	public String getMessage() {
		return ConfigUtil.INTERNAL_ERROR;
	}

}
