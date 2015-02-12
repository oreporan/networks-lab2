import static org.junit.Assert.*;

import org.junit.Test;


public class PoliciesUtilTest {

	@Test
	public void testMatchSubnet() {
	assertFalse(PoliciesUtil.matchSubnet("10.15.25.15", "11.15.25.5", "22"));
	assertTrue(PoliciesUtil.matchSubnet("10.15.24.5", "11.255.255.254", "6"));
	assertFalse(PoliciesUtil.matchSubnet("10.15.24.5", "7.15.24.255", "6"));
	assertTrue(PoliciesUtil.matchSubnet("1.0.0.127", "1.0.0.127", "6"));

	}

}
