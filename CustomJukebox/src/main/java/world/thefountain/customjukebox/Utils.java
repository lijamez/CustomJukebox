package world.thefountain.customjukebox;

import org.bukkit.permissions.Permissible;

public class Utils {

	private Utils() { }
	
	/**
	 * Asserts that the {@code permissible} has access to the {@code permissionNode}. 
	 * This method will not throw PermissionException if the {@code permissible} is null.
	 * 
	 * @param permissible {@link Permissible}.
	 * @param permissionNode Permission node name. 
	 * @throws PermissionException if there are no permissions. 
	 */
	public static void assertPermissions(Permissible permissible, String permissionNode) {
		
		if (permissible != null && !permissible.hasPermission(permissionNode)) {
			throw new PermissionException("You don't have permissions to do that.");
		}
	}
	
}
