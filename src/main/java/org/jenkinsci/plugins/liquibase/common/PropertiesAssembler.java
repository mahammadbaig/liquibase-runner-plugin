package org.jenkinsci.plugins.liquibase.common;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.IncludedDatabaseDriver;
import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class PropertiesAssembler {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesAssembler.class);
    private static final String DEFAULT_JDBC_URL = "jdbc:h2:mem:builder-db";
    private static final String DEFAULT_DB_DRIVER = "org.h2.Driver";

    /**
     * Creates a properties instance for use with liquibase execution.
     * @param liquibaseBuilder
     * @param build
     * @return
     */
    public static Properties createLiquibaseProperties(ChangesetEvaluator liquibaseBuilder, AbstractBuild<?, ?> build) {
        Properties properties = new Properties();
        assembleDefaults(properties);
        assembleFromProjectConfiguration(liquibaseBuilder, properties, build);
        return properties;
    }

    protected static void assembleFromProjectConfiguration(ChangesetEvaluator liquibaseBuilder,
                                                           Properties properties,
                                                           AbstractBuild<?, ?> build) {


        String liquibasePropertiesPath = liquibaseBuilder.getLiquibasePropertiesPath();
        readFromExternalProperties(properties, liquibasePropertiesPath, build);
        setIfNotNull(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile());
        setIfNotNull(properties, LiquibaseProperty.USERNAME, liquibaseBuilder.getUsername());
        setIfNotNull(properties, LiquibaseProperty.PASSWORD, liquibaseBuilder.getPassword());
        setIfNotNull(properties, LiquibaseProperty.DEFAULT_SCHEMA_NAME, liquibaseBuilder.getDefaultSchemaName());
        setIfNotNull(properties, LiquibaseProperty.URL, liquibaseBuilder.getUrl());
        setIfNotNull(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile());
        setIfNotNull(properties, LiquibaseProperty.LABELS, liquibaseBuilder.getLabels());
        setIfNotNull(properties, LiquibaseProperty.CONTEXTS, liquibaseBuilder.getContexts());
        resolveDatabaseDriver(liquibaseBuilder, properties);
    }

    private static void resolveDatabaseDriver(ChangesetEvaluator liquibaseBuilder, Properties properties) {
        if (!Strings.isNullOrEmpty(liquibaseBuilder.getDatabaseEngine())) {
            PropertiesAssembler.setDriverFromDBEngine(liquibaseBuilder, properties);
        } else {
            if (!Strings.isNullOrEmpty(liquibaseBuilder.getDriverClassname())) {
                setProperty(properties, LiquibaseProperty.DRIVER, liquibaseBuilder.getDriverClassname());
            }
        }
    }

    private static void readFromExternalProperties(Properties properties,
                                                   String liquibasePropertiesPath,
                                                   AbstractBuild<?, ?> build) {
        if (!Strings.isNullOrEmpty(liquibasePropertiesPath)) {
            InputStreamReader streamReader = null;
            try {
                FilePath liquibaseProperties = build.getWorkspace().child(liquibasePropertiesPath);

                streamReader = new InputStreamReader(liquibaseProperties.read());
                properties.load(streamReader);
            } catch (IOException e) {
                throw new LiquibaseRuntimeException(
                        "Unable to load properties file at '" + liquibasePropertiesPath + "'", e);
            } catch (InterruptedException e) {
                throw new LiquibaseRuntimeException(
                        "Unable to load properties file at '" + liquibasePropertiesPath + "'", e);

            } finally {
                IOUtils.closeQuietly(streamReader);
            }
        }
    }

    private static void assembleDefaults(Properties properties) {
        setProperty(properties, LiquibaseProperty.DRIVER, DEFAULT_DB_DRIVER);
        setProperty(properties, LiquibaseProperty.URL, DEFAULT_JDBC_URL);
    }

    private static void setProperty(Properties properties, LiquibaseProperty liquibaseProperty, String value) {
        properties.setProperty(liquibaseProperty.propertyName(), value);
    }

    private static void setIfNotNull(Properties properties,
                                     LiquibaseProperty liquibaseProperty,
                                     String value) {
        if (!Strings.isNullOrEmpty(value)) {
            setProperty(properties, liquibaseProperty, value);
        }
    }

    public static void setDriverFromDBEngine(ChangesetEvaluator changesetEvaluator, Properties properties) {
        if (!Strings.isNullOrEmpty(changesetEvaluator.getDatabaseEngine())) {
            for (IncludedDatabaseDriver includedDatabaseDriver : changesetEvaluator.getDrivers()) {
                if (includedDatabaseDriver.getDisplayName().equals(changesetEvaluator.getDatabaseEngine())) {
                    setProperty(properties, LiquibaseProperty.DRIVER, includedDatabaseDriver.getDriverClassName());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("using db driver class[" + includedDatabaseDriver.getDriverClassName() + "] ");
                    }
                    break;
                }
            }
        }
    }

}
