require "bundler/setup"
require "sorbet-runtime"
require "descriptive-statistics"

class Main
  extend T::Sig
  # typed: true

  def self.main
    times = ["30", "60", "180", "300"]
    modalities = ["NONE", "RANDOM", "ON_THE_FLY", "SHADOW_COPY"]
    ransomwares = [ "Globeimposter", "Grandcrab", "LockBit", "Phobos", "Ryuk", "Unlock92", "Vipasana", "WannaCry", "Xorist" ]

    folder = ARGV[0]
    if folder == nil or not File.directory? folder
      abort("'#{folder}' is not a folder, provide a path to an existing tests folder")
    end
    detailed_report = ARGV[1] == "debug"
    single_page = ARGV[1] == "single_page"
    $reset_cache = ARGV[1] == "reset_cache" || ARGV[2] == "reset_cache"

    baseprofile = read_report("#{folder}/profile.base")
    reports = load_reports(folder)

    missing_tests = Array.new
    results = Hash.new

    labels = [:lost, :saved, :copies]
    times.each do |time|
      results[time] = Array.new
      modalities.each do |modality|
        ransomwares.each do |ransomware|
          if reports[time][modality][ransomware].nil?
            missing_tests.push({ :time => time, :modality => modality, :ransomware => ransomware })
          else
            ag_rep = { :lost => [], :saved => [], :copies => [] }
            reports[time][modality][ransomware].each do |report|
              comp = compare(baseprofile, report)
              labels.each do |label|
                ag_rep[label].push(comp[label])
              end
            end
            labels.each do |label|
              data = ag_rep[label]
              # size = data.size
              # data.map!(&:to_f)
              # mean = data.reduce(&:+) / size
              # sum_sqr = data.map { |x| x * x }.reduce(&:+)
              # std_dev = Math.sqrt((sum_sqr - size * mean * mean) / (size - 1))
              data.extend( DescriptiveStatistics )
              ag_rep[extSym(label, :avg)] = data.mean
              ag_rep[extSym(label, :std_dev)] = data.standard_deviation
            end
            ag_rep[:total] = ag_rep[:lost_avg] + ag_rep[:saved_avg] + ag_rep[:copies_avg]
            labels.each do |label|
              ag_rep[extSym(label, :perc_avg)] = (ag_rep[extSym(label, :avg)].fdiv(ag_rep[:total]) * 100).round()
              ag_rep[extSym(label, :perc_std_dev)] = (ag_rep[extSym(label, :std_dev)].fdiv(ag_rep[:total]) * 100).round()
            end
            results[time].push({ :modality => modality, :ransomware => ransomware, :rep => ag_rep })
          end
        end
      end
    end

    if detailed_report
      max_st_dev = Array.new
      results.each do |time, rows|
        rows.each do |row|
          puts "#{time}, #{row[:modality]}, #{row[:ransomware]}"
          puts row[:rep]
          labels.each do |label|
              m = Hash.new
              m[:name] = "#{time}, #{row[:modality]}, #{row[:ransomware]}"
              m[:ransomware] = row[:ransomware]
              m[:measure] = label
              m[:std_dev] = row[:rep][extSym(label, :std_dev)]
              m[:perc_std_dev] = row[:rep][extSym(label, :perc_std_dev)]
              max_st_dev.push(m)
          end
        end
      end
      puts max_st_dev
      ransomwares.each do | r |
        acc = Array.new
        max_st_dev.select{ | m | m[ :ransomware ] == r }.each do | m |
          acc.push( m[ :perc_std_dev ] )
        end
        # size = acc.size
        # acc.map!(&:to_f)
        # mean = acc.reduce(&:+) / size
        # sum_sqr = acc.map { |x| x * x }.reduce(&:+)
        # std_dev = Math.sqrt((sum_sqr - size * mean * mean) / (size - 1))
        acc.extend(DescriptiveStatistics)
        puts "#{r}, #{acc.mean}, #{acc.standard_deviation}"
      end
      # max_st_dev = max_st_dev.sort_by { |m| m[:perc_std_dev] }
      # puts "\nST DEVS"
      # puts max_st_dev
      # puts "\n"
    else
      if single_page
        puts "\\begin{tabular}{@{} l c c c c c @{}} & None & Random & On-The-Fly & Shadow \\\\[.5em]"
        # puts "\\section*{#{time} Seconds Delay}"
        ransomwares.each do |ransomware|
          if (missing_tests.select { |item| item[:ransomware] == ransomware }).empty?
            row = "\\\\[1.5em]\\cEntry{#{ransomware}}"
            results.each do |time, rows|
              none = rows.select { |item| item[:modality] == "NONE" && item[:ransomware] == ransomware }[0][:rep]
              random = rows.select { |item| item[:modality] == "RANDOM" && item[:ransomware] == ransomware }[0][:rep]
              otf = rows.select { |item| item[:modality] == "ON_THE_FLY" && item[:ransomware] == ransomware }[0][:rep]
              shadow = rows.select { |item| item[:modality] == "SHADOW_COPY" && item[:ransomware] == ransomware }[0][:rep]
              row += " & \\CCa{#{none[:saved_perc_avg]}}{#{none[:copies_perc_avg]}}"
              row += " & \\CCb{#{random[:saved_perc_avg]}}{#{random[:copies_perc_avg]}}"
              row += " & \\CCc{#{otf[:saved_perc_avg]}}{#{otf[:copies_perc_avg]}}"
              row += " & \\CCd{#{shadow[:saved_perc_avg]}}{#{shadow[:copies_perc_avg]}}"
              if time != "300"
                row += "\n \\\\ "
              else
                row += "\n"
              end
            end
            puts row
          end
        end
        puts "\\end{tabular}"
      else
        results.each do |time, rows|
          puts "\\section*{#{time} Seconds Delay}"
          puts "\\begin{tabular}{@{} l c c c c c @{}} & None & Random & On-The-Fly & Shadow \\\\[.5em]"
          ransomwares.each do |ransomware|
            if (missing_tests.select { |item| item[:time] == time && item[:ransomware] == ransomware }).empty?
              none = rows.select { |item| item[:modality] == "NONE" && item[:ransomware] == ransomware }[0][:rep]
              random = rows.select { |item| item[:modality] == "RANDOM" && item[:ransomware] == ransomware }[0][:rep]
              otf = rows.select { |item| item[:modality] == "ON_THE_FLY" && item[:ransomware] == ransomware }[0][:rep]
              shadow = rows.select { |item| item[:modality] == "SHADOW_COPY" && item[:ransomware] == ransomware }[0][:rep]
              row = "\\\\[1.5em]\\cEntry{#{ransomware}}"
              row += " & \\CCa{#{none[:saved_perc_avg]}}{#{none[:copies_perc_avg]}}"
              row += " & \\CCb{#{random[:saved_perc_avg]}}{#{random[:copies_perc_avg]}}"
              row += " & \\CCc{#{otf[:saved_perc_avg]}}{#{otf[:copies_perc_avg]}}"
              row += " & \\CCd{#{shadow[:saved_perc_avg]}}{#{shadow[:copies_perc_avg]}}"
              puts row
            end
          end
          puts "\\end{tabular}"
        end
      end
    end

    puts "Missing tests"
    puts missing_tests
  end

  def self.watched_folders(line)
    watched_folders = ["DOCUMENT", "DOWNLOAD", "Desktop", "Documents", "Downloads", "Music", "My Documents", "OneDrive", "Pictures", "PrintHood", "SAVED_GA", "SendTo", "Videos"]
    watched_folders.each do |folder|
      if line.start_with? "#{folder}/"
        return true
      end
    end
    return false
  end

  def self.read_report(filepath)
    lines = File.open(filepath).read.split("\n")
    map = Hash.new
    #puts "Starts with #{lines.size}"
    lines.each do |l|
      if watched_folders(l)
        split = l.split(",")
        hash = split[1]
        file = split[0]
        getOrAddKey(map, hash, Array.new).push(file.strip)
      end
    end
    #puts "remaining lines #{map.size}"
    return map
  end

  def self.contains_ranflood_files(run)
    run.each do |f|
      if File.basename(f) =~ /.*_\w+\.\w+/
        return true
      end
    end
    return false
  end

  def self.compare(baseprofile, run)
    lost = 0
    saved = 0
    copied = 0
    baseprofile.each do |hash, file|
      if run.has_key? hash
        if (run[hash] & baseprofile[hash]).size > 0 || !contains_ranflood_files(run[hash])
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

  def self.show_loading(counter, index, total)
    r = (index.fdiv(total) * 100).ceil()
    if r.modulo(10) == 0 && (counter[:c] < r)
      print "#{r}%"
      counter[:c] = r
    elsif index.modulo(10) == 0
      print "."
    end
  end

  $cachefile = "tests.cache"

  sig { params( folder: String ).returns( Hash ) }
  def self.load_reports(folder)
    if $reset_cache and File.exists? $cachefile
      File.delete( $cachefile )
    end
    if File.exist? $cachefile
      puts "loading from cache #{$cachefile}"
      File.open( $cachefile, "r" ){ | f | return Marshal.load( f ) }
    else
      puts "loading from source #{folder}"
      reports = Hash.new
      files = Dir.glob("#{folder}/*.report")
      counter = { :c => -1 }
      files.each_with_index do |file, index|
        show_loading(counter, index, files.size)
        split = File.basename(file).split("-")
        time_h = getOrAddKey(reports, split[1], Hash.new)
        modality_h = getOrAddKey(time_h, split[2], Hash.new)
        ransomware_a = getOrAddKey(modality_h, split[0], Array.new)
        ransomware_a.push(read_report(file))
      end
      File.open( $cachefile, "w" ){ | f | Marshal.dump( reports, f ) }
      puts ""
      return reports
    end
  end

  sig do 
    type_parameters( :K, :V )
    .params( hash: T::Hash[ T.type_parameter( :K ), T.type_parameter( :V ) ], key: T.type_parameter( :K ), default: T.type_parameter( :V ) )
    .returns( T.type_parameter( :V ) )
  end
  def self.getOrAddKey(hash, key, default)
    if !hash.has_key? key
      hash[key] = default
    end
    return hash[key]
  end

  sig { params( symbol: Symbol, suffix: Symbol ).returns( Symbol ) }
  def self.extSym( symbol, suffix )
    "#{symbol.to_s}_#{suffix.to_s}".to_sym
  end
end

Main.main