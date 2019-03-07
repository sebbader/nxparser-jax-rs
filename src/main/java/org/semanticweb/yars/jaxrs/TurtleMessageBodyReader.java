package org.semanticweb.yars.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.kohsuke.MetaInfServices;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.turtle.TurtleParser;
import org.semanticweb.yars.utils.CallbackIterator;
import org.semarglproject.rdf.TurtleSerializer;
import org.semarglproject.sink.CharOutputSink;
import org.semarglproject.sink.CharSink;
import org.semarglproject.sink.TripleSink;

/**
 * A {@link MessageBodyReader} and {@link MessageBodyWriter} for <a
 * href="http://www.w3.org/TR/turtle/">Turtle</a>.
 *
 * @author Tobias
 * @see AbstractRDFMessageBodyReaderWriter
 *
 */
@Consumes({ "text/turtle" })
@Produces({ "text/turtle" })
@Provider
@MetaInfServices({ MessageBodyWriter.class, MessageBodyReader.class })
public class TurtleMessageBodyReader extends AbstractRDFMessageBodyReaderWriter {

	@Context
	UriInfo _uriinfo;

	@Override
	boolean isReadableCheckMediatypeAndAnnotations(Annotation[] annotations,
			MediaType mt) {
		return NxMessageBodyReaderWriter.TURTLE_MEDIATYPE.isCompatible(mt);
	}

	@Override
	boolean isWritableCheckMediatypeAndAnnotations(Annotation[] annotations,
			MediaType mt) {
		return NxMessageBodyReaderWriter.TURTLE_MEDIATYPE.isCompatible(mt);
	}

	@Override
	public void writeTo(Iterable<Node[]> arg0, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		CharOutputSink cs = new CharOutputSink(getCharset(mediaType));
		
		//sba start:
		if (_uriinfo == null) {
			cs.setBaseUri(org.semanticweb.yars.util.Util.THIS_URI.toString());
		} else {
			cs.setBaseUri(_uriinfo.getAbsolutePath().toString());
		}
		//sba end
		
		cs.connect(entityStream);
		TripleSink ts = TurtleSerializer.connect((CharSink) cs);

		try {
			ts.startStream();

			for (Node[] nx : arg0) {

				if (!(nx[2] instanceof Literal))
					// it's not a literal
					ts.addNonLiteral(
							nx[0] instanceof BNode ? nx[0].toString() : nx[0]
									.getLabel(),
							nx[1].getLabel(),
							nx[2] instanceof BNode ? nx[2].toString() : nx[2]
									.getLabel());
				else {
					// it's a literal
					Literal l = (Literal) nx[2];
					if (l.getDatatype() != null)
						ts.addTypedLiteral(
								nx[0] instanceof BNode ? nx[0].toString()
										: nx[0].getLabel(), nx[1].getLabel(),
								nx[2].getLabel(), l.getDatatype().getLabel());
					else
						ts.addPlainLiteral(
								nx[0] instanceof BNode ? nx[0].toString()
										: nx[0].getLabel(), nx[1].getLabel(),
								nx[2].getLabel(), l.getLanguageTag());

				}
			}

			ts.endStream();

		} catch (org.semarglproject.rdf.ParseException e) {
			throw new WebApplicationException(e.getCause());
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new WebApplicationException(e.getCause());
		}

	}

	@Override
	public Iterable<Node[]> readFrom(Class<Iterable<Node[]>> arg0,
			Type genericType, Annotation annotations[], MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {

		TurtleParser parser = new TurtleParser(entityStream,
				getCharset(mediaType), getBaseURIdependingOnPostOrNot(httpHeaders));

		CallbackIterator cs = new CallbackIterator();

		try {
			parser.parse(cs);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParseException e) {
			throw new BadRequestException(e);
		}

		try {
			cs.hasNext();
		} catch (Exception e) {
			throw new WebApplicationException(e.getCause());
		}

		return cs;
	}

}
