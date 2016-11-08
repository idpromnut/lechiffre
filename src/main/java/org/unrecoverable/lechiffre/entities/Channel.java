package org.unrecoverable.lechiffre.entities;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@JsonInclude
@Data
@EqualsAndHashCode(of={"id"})
public class Channel {

	@NonNull
	private String id;

	@NonNull
	private String name;
}
