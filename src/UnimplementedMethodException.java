
public class UnimplementedMethodException extends WebServerException {

	private static final long serialVersionUID = -7120805035176053554L;

	@Override
	public String getMessage() {
		return ConfigUtil.UNIMPLEMENTED_METHOD;
	}

}
