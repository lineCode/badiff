package org.badiff.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public abstract class Arguments {

	public static class DiffArguments extends AbstractArguments {
		public static final String PIPELINE = "pipeline";
		public static final String OUTPUT = "output";
		public static final String BEFORE = "before";
		public static final String AFTER = "after";

		private DiffArguments() {
			super("diff_args.properties");
			opt("p", PIPELINE, true, "diff pipeline code to use");
			req("o", OUTPUT, true, "output file");
			req("1", BEFORE, true, "original file for comparison");
			req("2", AFTER, true, "target file for comparison");
		}
	}
	
	public static final DiffArguments DIFF = new DiffArguments();
	
	public static abstract class AbstractArguments extends Options {
		protected Properties defaults;
		
		public AbstractArguments(String rsrc) {
			defaults = new Properties();
			try {
				InputStream in = Arguments.class.getResourceAsStream(rsrc);
				try {
					defaults.load(in);
				} finally {
					in.close();
				}
			} catch(IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
		
		public CommandLine parse(String... args) throws ParseException {
			return new PosixParser().parse(this, args, defaults);
		}
		
		protected void opt(String opt, String longOpt, boolean hasArg, String desc) {
			Option o = new Option(opt, longOpt, hasArg, desc);
			addOption(o);
		}
		
		protected void req(String opt, String longOpt, boolean hasArg, String desc) {
			Option o = new Option(opt, longOpt, hasArg, desc);
			o.setRequired(true);
			addOption(o);
		}
		
		public Properties getDefaults() {
			return defaults;
		}
	}
	
	private Arguments() {}
}
