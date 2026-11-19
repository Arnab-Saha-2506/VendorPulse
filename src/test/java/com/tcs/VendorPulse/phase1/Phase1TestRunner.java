package com.tcs.VendorPulse.phase1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class Phase1TestRunner {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  RUNNING PHASE 1 UNIT TESTS                      ");
        System.out.println("==================================================");

        List<Class<?>> testClasses = List.of(
                AdminVerificationTest.class,
                AgreementImmutabilityAndOwnershipTest.class,
                SupplierRegistrationAndDocumentSecurityTest.class
        );

        int total = 0;
        int passed = 0;
        int failed = 0;

        for (Class<?> clazz : testClasses) {
            System.out.println("\nExecuting: " + clazz.getSimpleName());
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    total++;
                    try {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        // Initialize @Mock fields if any
                        org.mockito.MockitoAnnotations.openMocks(instance);

                        // Run @BeforeEach
                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.isAnnotationPresent(BeforeEach.class)) {
                                m.setAccessible(true);
                                m.invoke(instance);
                            }
                        }

                        method.setAccessible(true);
                        method.invoke(instance);
                        passed++;
                        System.out.println("  [PASS] " + method.getName());
                    } catch (Throwable t) {
                        failed++;
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        System.err.println("  [FAIL] " + method.getName() + " -> " + cause.getMessage());
                        cause.printStackTrace();
                    }
                }
            }
        }

        System.out.println("\n==================================================");
        System.out.println("TEST RESULTS: Total: " + total + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}

