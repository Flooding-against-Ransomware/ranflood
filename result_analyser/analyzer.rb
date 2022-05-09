END { 
times = [ "30", "60", "180", "300" ]
modalities = [ "NONE", "RANDOM", "ON_THE_FLY", "SHADOW_COPY" ]
ransomwares = [ "Globeimposter", "Grandcrab", "LockBit", "Phobos", "Ryuk", "Unlock92", "Vipasana", "WannaCry", "Xorist" ]

baseprofile = read_report( "tests/profile.base" )
reports = load_reports()


missing_tests = Array.new
results = Hash.new
detailed_report = ARGV[ 0 ] == "debug"

times.each do | time |
  results[ time ] = Array.new
  modalities.each do | modality |
    ransomwares.each do | ransomware |
      if reports[ time ][ modality ][ ransomware ].nil? 
        missing_tests.push( { :time => time, :modality => modality, :ransomware => ransomware } )
      else
        ag_rep = nil
        reports[ time ][ modality ][ ransomware ].each do | report |
          comp = compare( baseprofile, report )
          if ag_rep.nil?
            ag_rep = comp
            ag_rep[ :count ] = 1
          else
            ag_rep[ :lost ] += comp[ :lost ]
            ag_rep[ :saved ] += comp[ :saved ]
            ag_rep[ :copies ] += comp[ :copies ]
            ag_rep[ :count ] += 1
          end
        end
        ag_rep[ :lost ] = ag_rep[ :lost ] / ag_rep[ :count ]
        ag_rep[ :saved ] = ag_rep[ :saved ] / ag_rep[ :count ]
        ag_rep[ :copies ] = ag_rep[ :copies ] / ag_rep[ :count ]
        ag_rep[ :total ] = ag_rep[ :lost ] + ag_rep[ :saved ] + ag_rep[ :copies ]
        ag_rep[ :lost_perc ] = ( ag_rep[ :lost ].fdiv( ag_rep[ :total ] ) * 100 ).round()
        ag_rep[ :saved_perc ] = ( ag_rep[ :saved ].fdiv( ag_rep[ :total ] ) * 100 ).round()
        ag_rep[ :copies_perc ] = ( ag_rep[ :copies ].fdiv( ag_rep[ :total ] ) * 100 ).round()
        results[ time ].push( { :modality => modality, :ransomware => ransomware, :rep => ag_rep } )
      end
    end
  end
end

if detailed_report
  results.each do | time, rows |
    rows.each do | row |
      puts "#{time}, #{row[:modality]}, #{row[:ransomware]}"
      puts row[ :rep ]
    end
  end
else
  results.each do | time, rows |
    puts "\\section*{#{time} Seconds Delay}"
    puts "\\begin{tabular}{@{} l c c c c c @{}} & None & Random & On-The-Fly & Shadow \\\\[.5em]"
    ransomwares.each do | ransomware |
      if ( missing_tests.select{ | item | item[ :time ] == time && item[ :ransomware ] == ransomware } ).empty?
        none = rows.select{   | item | item[ :modality ] == "NONE"        && item[ :ransomware ] == ransomware }[ 0 ][ :rep ]
        random = rows.select{ | item | item[ :modality ] == "RANDOM"      && item[ :ransomware ] == ransomware }[ 0 ][ :rep ]
        otf = rows.select{    | item | item[ :modality ] == "ON_THE_FLY"  && item[ :ransomware ] == ransomware }[ 0 ][ :rep ]
        shadow = rows.select{   | item | item[ :modality ] == "SHADOW_COPY" && item[ :ransomware ] == ransomware }[ 0 ][ :rep ]
        row = "\\\\[1.5em]\\cEntry{#{ransomware}}"
        row += " & \\CCa{#{none[ :saved_perc ]}}{#{none[ :copies_perc ]}}"
        row += " & \\CCb{#{random[ :saved_perc ]}}{#{random[ :copies_perc ]}}"
        row += " & \\CCc{#{otf[ :saved_perc ]}}{#{otf[ :copies_perc ]}}"
        row += " & \\CCd{#{shadow[ :saved_perc ]}}{#{shadow[ :copies_perc ]}}"
        puts row
      end
    end
    puts "\\end{tabular}"
  end
end

puts "Missing tests"
puts missing_tests

}


def watched_folders( line )
  watched_folders = [ "DOCUMENT", "DOWNLOAD", "Desktop", "Documents", "Downloads", "Music", "My Documents", "OneDrive", "Pictures", "PrintHood", "SAVED_GA", "SendTo", "Videos" ]
  watched_folders.each do | folder |
    if line.start_with? "#{folder}/"
      return true
    end
  end
  return false
end

def read_report( filepath )
  lines = File.open( filepath ).read.split( "\n" )
  map = Hash.new
  #puts "Starts with #{lines.size}"
  lines.each do | l |
    if watched_folders( l )
      split = l.split( "," )
      hash = split[ 1 ]
      file = split[ 0 ]
      getOrAddKey( map, hash, Array.new ).push( file.strip )
    end
  end
  #puts "remaining lines #{map.size}"
  return map
end

def contains_ranflood_files( run )
  run.each do | f |
    if File.basename( f ) =~ /.*_\w+\.\w+/
      return true
    end
  end
  return false
end

def compare( baseprofile, run )
  lost=0
  saved=0
  copied=0
  baseprofile.each do | hash, file |
    if run.has_key? hash
      if ( run[ hash ] & baseprofile[ hash ] ).size > 0 || ! contains_ranflood_files( run[ hash ] )
        saved += 1
      else
        copied += 1
      end
    else
      lost += 1
    end
  end
  return { :lost => lost, :saved => saved, :copies => copied }
end

def show_loading( counter, index, total )
  r = ( index.fdiv( total ) * 100 ).ceil()
  if r.modulo( 10 ) == 0 && ( counter[ :c ] < r )
    print "#{r}%"
    counter[ :c ] = r
  elsif index.modulo( 10 ) == 0
    print "."
  end
end

def load_reports()
  reports = Hash.new
  files = Dir.glob( "tests/*.report" )
  counter = { :c => -1 }
  files.each_with_index do | file, index |
    show_loading( counter, index, files.size )
    split = File.basename( file ).split( "-" )
    time_h = getOrAddKey( reports, split[ 1 ], Hash.new )
    modality_h = getOrAddKey( time_h, split[ 2 ], Hash.new )
    ransomware_a = getOrAddKey( modality_h, split[ 0 ], Array.new )
    ransomware_a.push( read_report( file ) )
  end
  puts ""
  return reports
end

def getOrAddKey( hash, key, default )
  if ! hash.has_key? key
    hash[ key ] = default
  end
  return hash[ key ]
end