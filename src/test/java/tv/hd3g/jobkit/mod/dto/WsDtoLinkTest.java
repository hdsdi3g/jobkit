/*
 * This file is part of AuthKit.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.jobkit.mod.dto;

import static com.github.javafaker.Faker.instance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.UriTemplate;
import org.springframework.web.bind.annotation.RequestMethod;

import tv.hd3g.testtools.HashCodeEqualsTest;

class WsDtoLinkTest extends HashCodeEqualsTest {

	@Mock
	private Link link;
	@Mock
	private UriTemplate uriTemplate;

	private String rel;
	private RequestMethod method;
	private WsDtoLink ws;

	@BeforeEach
	void init() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(link.getTemplate()).thenReturn(uriTemplate);
		rel = RandomStringUtils.randomAscii(5000, 10000);
		Mockito.when(link.getRel()).thenReturn(rel);
		method = getRandomEnum(RequestMethod.class);

		ws = new WsDtoLink(link, method);
	}

	@Test
	void testGetAction() {
		assertEquals(method.name(), ws.getAction());
	}

	@Test
	void testGetTemplate() {
		assertEquals(uriTemplate, ws.getTemplate());
	}

	@Test
	void testGetRel() {
		assertEquals(rel, ws.getRel());
	}

	@Override
	protected Object[] makeSameInstances() {
		return new Object[] { new WsDtoLink(link, method), new WsDtoLink(link, method) };
	}

	static <T extends Enum<?>> T getRandomEnum(final Class<T> enum_class) {
		final int x = instance().random().nextInt(enum_class.getEnumConstants().length);
		return enum_class.getEnumConstants()[x];
	}

}
