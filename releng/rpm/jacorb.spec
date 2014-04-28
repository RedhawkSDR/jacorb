name:             jacorb
Version:          3.3.0
Release:          1%{?dist}
Summary:          The Java implementation of the OMG's CORBA standard
Group:            Development/Libraries
License:          LGPLv2
URL:              http://www.jacorb.org/index.html
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-buildroot
Source:         %{name}-%{version}.tar.gz

Requires:       java >= 1.6

%description
This package contains the Java implementation of the OMG's CORBA standard

%prep
%setup

%install
mkdir -p $RPM_BUILD_ROOT%{_javadir}/%{name}
cp -r * $RPM_BUILD_ROOT%{_javadir}/%{name}

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-, root, root)
%{_javadir}/*
