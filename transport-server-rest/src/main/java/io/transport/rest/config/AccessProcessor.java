package io.transport.rest.config;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Access configuration processor.
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0
 * @date 2018年5月24日
 * @since
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AccessProcessor implements InitializingBean {
	final private static Logger logger = LoggerFactory.getLogger(AccessProcessor.class);

	@Value("${rest.auth.allow-ip}")
	private String allowIp;
	@Value("${rest.auth.disable-ip:}") // 可不设置时使用默认EMPTY
	private String disableIp;
	@Value("${rest.auth.users}")
	private String users;

	private Set<String> userList = new HashSet<>();
	private Set<IPRange> allowList = new HashSet<>();
	private Set<IPRange> denyList = new HashSet<>();

	public String getAllowIp() {
		return allowIp;
	}

	public void setAllowIp(String allowIp) {
		this.allowIp = allowIp;
	}

	public String getDisableIp() {
		return disableIp;
	}

	public void setDisableIp(String disableIp) {
		this.disableIp = disableIp;
	}

	public String getUsers() {
		return users;
	}

	public void setUsers(String users) {
		this.users = users;
	}

	public Set<String> getUserList() {
		return userList;
	}

	public void setUserList(Set<String> userList) {
		this.userList = userList;
	}

	public Set<IPRange> getAllowList() {
		return allowList;
	}

	public void setAllowList(Set<IPRange> allowList) {
		this.allowList = allowList;
	}

	public Set<IPRange> getDenyList() {
		return denyList;
	}

	public void setDenyList(Set<IPRange> denyList) {
		this.denyList = denyList;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Allow list.
		try {
			String allows = this.getAllowIp();
			if (allows != null && allows.trim().length() != 0) {
				allows = allows.trim();
				String[] items = allows.split(",");

				for (String item : items) {
					if (item == null || item.length() == 0)
						continue;
					this.getAllowList().add(new IPRange(item));
				}
			}
		} catch (Exception e) {
			String msg = "Init config error, allow : " + this.getDisableIp();
			logger.error(msg, e);
		}

		// Deny list.
		try {
			String denys = this.getDisableIp();
			if (denys != null && denys.trim().length() != 0) {
				denys = denys.trim();
				String[] items = denys.split(",");

				for (String item : items) {
					if (item == null || item.length() == 0)
						continue;
					this.getDenyList().add(new IPRange(item));
				}
			}
		} catch (Exception e) {
			String msg = "Init config error, deny : " + this.getDisableIp();
			logger.error(msg, e);
		}

		// Users to User list.
		try {
			for (String u : this.getUsers().split(",")) {
				this.getUserList().add(u);
			}
		} catch (Exception e) {
			String msg = "Init config error, users : " + this.getUsers();
			logger.error(msg, e);
		}
	}

	/**
	 * Access authorization check
	 * 
	 * @param accessAddr
	 * @return
	 */
	public boolean isPermittedRequest(String accessAddr) {
		boolean ipV6 = accessAddr != null && accessAddr.indexOf(':') != -1;
		if (ipV6)
			return "0:0:0:0:0:0:0:1".equals(accessAddr)
					|| (this.getDenyList().size() == 0 && this.getAllowList().size() == 0);

		IPAddress ipAddress = new IPAddress(accessAddr);
		for (IPRange range : this.getDenyList()) {
			if (range.isIPAddressInRange(ipAddress))
				return false;
		}
		if (this.getAllowList().size() > 0) {
			for (IPRange range : this.getAllowList()) {
				if (range.isIPAddressInRange(ipAddress))
					return true;
			}
			return false;
		}
		return true;
	}

	/*
	 * Copyright 1999-2101 Wangl.sir Group Holding Ltd.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License.
	 */
	static class IPAddress implements Cloneable {

		/** IP address */
		protected int ipAddress = 0;

		public IPAddress(String ipAddressStr) {
			ipAddress = parseIPAddress(ipAddressStr);
		}

		public IPAddress(int address) {
			ipAddress = address;
		}

		// -------------------------------------------------------------------------
		/**
		 * Return the integer representation of the IP address.
		 * 
		 * @return The IP address.
		 */
		public final int getIPAddress() {
			return ipAddress;
		}

		// -------------------------------------------------------------------------
		/**
		 * Return the string representation of the IP Address following the
		 * common decimal-dotted notation xxx.xxx.xxx.xxx.
		 * 
		 * @return Return the string representation of the IP address.
		 */
		public String toString() {
			StringBuilder result = new StringBuilder();
			int temp;

			temp = ipAddress & 0x000000FF;
			result.append(temp);
			result.append(".");

			temp = (ipAddress >> 8) & 0x000000FF;
			result.append(temp);
			result.append(".");

			temp = (ipAddress >> 16) & 0x000000FF;
			result.append(temp);
			result.append(".");

			temp = (ipAddress >> 24) & 0x000000FF;
			result.append(temp);

			return result.toString();
		}

		// -------------------------------------------------------------------------
		/**
		 * Check if the IP address is belongs to a Class A IP address.
		 * 
		 * @return Return <code>true</code> if the encapsulated IP address
		 *         belongs to a class A IP address, otherwise returne
		 *         <code>false</code>.
		 */
		public final boolean isClassA() {
			return (ipAddress & 0x00000001) == 0;
		}

		// -------------------------------------------------------------------------
		/**
		 * Check if the IP address is belongs to a Class B IP address.
		 * 
		 * @return Return <code>true</code> if the encapsulated IP address
		 *         belongs to a class B IP address, otherwise returne
		 *         <code>false</code>.
		 */
		public final boolean isClassB() {
			return (ipAddress & 0x00000003) == 1;
		}

		// -------------------------------------------------------------------------
		/**
		 * Check if the IP address is belongs to a Class C IP address.
		 * 
		 * @return Return <code>true</code> if the encapsulated IP address
		 *         belongs to a class C IP address, otherwise returne
		 *         <code>false</code>.
		 */
		public final boolean isClassC() {
			return (ipAddress & 0x00000007) == 3;
		}

		// -------------------------------------------------------------------------
		/**
		 * Convert a decimal-dotted notation representation of an IP address
		 * into an 32 bits interger value.
		 * 
		 * @param ipAddressStr
		 *            Decimal-dotted notation (xxx.xxx.xxx.xxx) of the IP
		 *            address.
		 * @return Return the 32 bits integer representation of the IP address.
		 * @exception InvalidIPAddressException
		 *                Throws this exception if the specified IP address is
		 *                not compliant to the decimal-dotted notation
		 *                xxx.xxx.xxx.xxx.
		 */
		final int parseIPAddress(String ipAddressStr) {
			int result = 0;

			if (ipAddressStr == null) {
				throw new IllegalArgumentException();
			}

			try {
				String tmp = ipAddressStr;

				// get the 3 first numbers
				int offset = 0;
				for (int i = 0; i < 3; i++) {

					// get the position of the first dot
					int index = tmp.indexOf('.');

					// if there is not a dot then the ip string representation
					// is
					// not compliant to the decimal-dotted notation.
					if (index != -1) {

						// get the number before the dot and convert it into
						// an integer.
						String numberStr = tmp.substring(0, index);
						int number = Integer.parseInt(numberStr);
						if ((number < 0) || (number > 255)) {
							throw new IllegalArgumentException("Invalid IP Address [" + ipAddressStr + "]");
						}

						result += number << offset;
						offset += 8;
						tmp = tmp.substring(index + 1);
					} else {
						throw new IllegalArgumentException("Invalid IP Address [" + ipAddressStr + "]");
					}
				}

				// the remaining part of the string should be the last number.
				if (tmp.length() > 0) {
					int number = Integer.parseInt(tmp);
					if ((number < 0) || (number > 255)) {
						throw new IllegalArgumentException("Invalid IP Address [" + ipAddressStr + "]");
					}

					result += number << offset;
					ipAddress = result;
				} else {
					throw new IllegalArgumentException("Invalid IP Address [" + ipAddressStr + "]");
				}
			} catch (NoSuchElementException ex) {
				throw new IllegalArgumentException("Invalid IP Address [" + ipAddressStr + "]", ex);
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid IP Address [" + ipAddressStr + "]", ex);
			}

			return result;
		}

		public int hashCode() {
			return this.ipAddress;
		}

		public boolean equals(Object another) {
			return another instanceof IPAddress && ipAddress == ((IPAddress) another).ipAddress;
		}
	}

	/*
	 * Copyright 1999-2101 Wangl.sir Group Holding Ltd.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License. <br/> <br/><br/>This class represents an IP Range,
	 * which are represented by an IP address and and a subnet mask. The
	 * standards describing modern routing protocols often refer to the
	 * extended-network-prefix-length rather than the subnet mask. The prefix
	 * length is equal to the number of contiguous one-bits in the traditional
	 * subnet mask. This means that specifying the network address 130.5.5.25
	 * with a subnet mask of 255.255.255.0 can also be expressed as
	 * 130.5.5.25/24. The prefix-length notation is more compact and easier to
	 * understand than writing out the mask in its traditional dotted-decimal
	 * format.
	 * 
	 * @author Marcel Dullaart
	 * 
	 * @version 1.0
	 * 
	 * @see IPAddress
	 */
	static class IPRange {

		/** IP address */
		private IPAddress ipAddress = null;

		/** IP subnet mask */
		private IPAddress ipSubnetMask = null;

		/** extended network prefix */
		private int extendedNetworkPrefix = 0;

		public IPRange(String range) {
			parseRange(range);
		}

		// -------------------------------------------------------------------------
		/**
		 * Return the encapsulated IP address.
		 * 
		 * @return The IP address.
		 */
		public final IPAddress getIPAddress() {
			return ipAddress;
		}

		// -------------------------------------------------------------------------
		/**
		 * Return the encapsulated subnet mask
		 * 
		 * @return The IP range's subnet mask.
		 */
		public final IPAddress getIPSubnetMask() {
			return ipSubnetMask;
		}

		// -------------------------------------------------------------------------
		/**
		 * Return the extended extended network prefix.
		 * 
		 * @return Return the extended network prefix.
		 */
		public final int getExtendedNetworkPrefix() {
			return extendedNetworkPrefix;
		}

		// -------------------------------------------------------------------------
		/**
		 * Convert the IP Range into a string representation.
		 * 
		 * @return Return the string representation of the IP Address following
		 *         the common format xxx.xxx.xxx.xxx/xx (IP address/extended
		 *         network prefixs).
		 */
		public String toString() {
			return ipAddress.toString() + "/" + extendedNetworkPrefix;
		}

		// -------------------------------------------------------------------------
		/**
		 * Parse the IP range string representation.
		 * 
		 * @param range
		 *            String representation of the IP range.
		 * @exception IllegalArgumentException
		 *                Throws this exception if the specified range is not a
		 *                valid IP network range.
		 */
		final void parseRange(String range) {
			if (range == null) {
				throw new IllegalArgumentException("Invalid IP range");
			}

			int index = range.indexOf('/');
			String subnetStr = null;
			if (index == -1) {
				ipAddress = new IPAddress(range);
			} else {
				ipAddress = new IPAddress(range.substring(0, index));
				subnetStr = range.substring(index + 1);
			}

			// try to convert the remaining part of the range into a decimal
			// value.
			try {
				if (subnetStr != null) {
					extendedNetworkPrefix = Integer.parseInt(subnetStr);
					if ((extendedNetworkPrefix < 0) || (extendedNetworkPrefix > 32)) {
						throw new IllegalArgumentException("Invalid IP range [" + range + "]");
					}
					ipSubnetMask = computeMaskFromNetworkPrefix(extendedNetworkPrefix);
				}
			} catch (NumberFormatException ex) {

				// the remaining part is not a valid decimal value.
				// Check if it's a decimal-dotted notation.
				ipSubnetMask = new IPAddress(subnetStr);

				// create the corresponding subnet decimal
				extendedNetworkPrefix = computeNetworkPrefixFromMask(ipSubnetMask);
				if (extendedNetworkPrefix == -1) {
					throw new IllegalArgumentException("Invalid IP range [" + range + "]", ex);
				}
			}
		}

		// -------------------------------------------------------------------------
		/**
		 * Compute the extended network prefix from the IP subnet mask.
		 * 
		 * @param mask
		 *            Reference to the subnet mask IP number.
		 * @return Return the extended network prefix. Return -1 if the
		 *         specified mask cannot be converted into a extended prefix
		 *         network.
		 */
		private int computeNetworkPrefixFromMask(IPAddress mask) {

			int result = 0;
			int tmp = mask.getIPAddress();

			while ((tmp & 0x00000001) == 0x00000001) {
				result++;
				tmp = tmp >>> 1;
			}

			if (tmp != 0) {
				return -1;
			}

			return result;
		}

		public static String toDecimalString(String inBinaryIpAddress) {
			StringBuilder decimalIp = new StringBuilder();
			String[] binary = new String[4];

			for (int i = 0, c = 0; i < 32; i = i + 8, c++) {
				binary[c] = inBinaryIpAddress.substring(i, i + 8);
				int octet = Integer.parseInt(binary[c], 2);
				decimalIp.append(octet);
				if (c < 3) {

					decimalIp.append('.');
				}
			}
			return decimalIp.toString();
		}

		// -------------------------------------------------------------------------
		/**
		 * Convert a extended network prefix integer into an IP number.
		 * 
		 * @param prefix
		 *            The network prefix number.
		 * @return Return the IP number corresponding to the extended network
		 *         prefix.
		 */
		private IPAddress computeMaskFromNetworkPrefix(int prefix) {

			/*
			 * int subnet = 0; for (int i=0; i<prefix; i++) { subnet = subnet <<
			 * 1; subnet += 1; }
			 */

			StringBuilder str = new StringBuilder();
			for (int i = 0; i < 32; i++) {
				if (i < prefix) {
					str.append("1");
				} else {
					str.append("0");
				}
			}

			String decimalString = toDecimalString(str.toString());
			return new IPAddress(decimalString);

		}

		// -------------------------------------------------------------------------
		/**
		 * Check if the specified IP address is in the encapsulated range.
		 * 
		 * @param address
		 *            The IP address to be tested.
		 * @return Return <code>true</code> if the specified IP address is in
		 *         the encapsulated IP range, otherwise return
		 *         <code>false</code>.
		 */
		public boolean isIPAddressInRange(IPAddress address) {
			if (ipSubnetMask == null) {
				return this.ipAddress.equals(address);
			}

			int result1 = address.getIPAddress() & ipSubnetMask.getIPAddress();
			int result2 = ipAddress.getIPAddress() & ipSubnetMask.getIPAddress();

			return result1 == result2;
		}
	}

}