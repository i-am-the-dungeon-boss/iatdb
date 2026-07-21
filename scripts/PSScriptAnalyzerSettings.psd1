@{
    Severity     = @('Error', 'Warning', 'Information')
    ExcludeRules = @(
        # Interactive CLI progress belongs on the host stream.
        'PSAvoidUsingWriteHost'
    )
}
