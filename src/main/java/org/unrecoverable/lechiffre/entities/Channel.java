package org.unrecoverable.lechiffre.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(of={"id"})
@AllArgsConstructor
public class Channel {

	@NonNull
	private String id;

	@NonNull
	private String name;
	
	private boolean voice;
}
