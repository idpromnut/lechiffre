package org.unecoverable.lechiffre.entities;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@JsonInclude
@Data
@EqualsAndHashCode(of={"id"})
public class User {

	@NonNull
	private String id;

	@NonNull
	private String name;

	@NonNull
	private String discriminator;
}
