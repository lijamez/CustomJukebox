package world.thefountain.customjukebox;

import org.bukkit.permissions.Permissible;

import com.google.common.base.Preconditions;

public class Utils {

	private Utils() { }
	
	/**
	 * Asserts that the {@code permissible} has access to the {@code permissionNode}. 
	 * This method will not throw PermissionException if the {@code permissible} is null.
	 * 
	 * @param permissible {@link Permissible}.
	 * @param permissionNode Permission node name. Non-null.
	 * @throws PermissionException if there are no permissions. 
	 */
	public static void assertPermissions(Permissible permissible, String permissionNode) {
		Preconditions.checkNotNull(permissionNode, "permissionNode must be non-null.");
		
		if (permissible != null && !permissible.hasPermission(permissionNode)) {
			throw new PermissionException("You don't have permissions to do that.");
		}
	}
	
}
